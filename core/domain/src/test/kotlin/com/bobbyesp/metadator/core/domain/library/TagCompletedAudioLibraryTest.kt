/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.library

import app.cash.turbine.test
import com.bobbyesp.metadator.core.domain.FakeLibrary
import com.bobbyesp.metadator.core.domain.FakeTagFiles
import com.bobbyesp.metadator.core.domain.FakeTrackTagCache
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackField
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.library.api.FileTrackTags
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagCompletedAudioLibraryTest {
    private fun track(id: Long, year: Int? = null, modified: Long = 1) =
        Track(
            id = TrackId(id),
            ref = ContentRef("content://$id"),
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            albumArtist = "Artist",
            albumId = 1,
            durationMs = 1000,
            trackNumber = 1,
            discNumber = null,
            year = year,
            genre = "Pop",
            mimeType = "audio/flac",
            sizeBytes = 10,
            displayName = "$id.flac",
            folder = "Music/",
            dateAddedEpochSeconds = 1,
            dateModifiedEpochSeconds = modified,
            artworkRef = null,
        )

    private fun file(vararg tags: Pair<String, List<String>>) =
        TagSnapshot(TagMap.of(*tags), emptyList())

    @Test
    fun `a year the library lacks comes from the file's date`() = runTest {
        val song = track(1)
        val files = FakeTagFiles(mapOf(song.ref to file("DATE" to listOf("2025-11-07"))))
        val library =
            TagCompletedAudioLibrary(FakeLibrary(listOf(song)), files, FakeTrackTagCache())

        library.observeTracks().test {
            assertTrue(awaitItem().single().needsAttention)
            val completed = awaitItem().single()
            assertEquals(2025, completed.year)
            assertFalse(completed.needsAttention)
        }
    }

    @Test
    fun `a field the file lacks too stays missing`() = runTest {
        val song = track(1)
        val files = FakeTagFiles(mapOf(song.ref to file("TITLE" to listOf("Song"))))
        val library =
            TagCompletedAudioLibrary(FakeLibrary(listOf(song)), files, FakeTrackTagCache())

        library.observeTracks().test {
            awaitItem()
            assertEquals(setOf(TrackField.Year), awaitItem().single().missingFields)
        }
    }

    @Test
    fun `what the library knows is kept and its file is not opened`() = runTest {
        val song = track(1, year = 1999)
        val files = FakeTagFiles(mapOf(song.ref to file("DATE" to listOf("2025"))))
        val library =
            TagCompletedAudioLibrary(FakeLibrary(listOf(song)), files, FakeTrackTagCache())

        library.observeTracks().test { assertEquals(1999, awaitItem().single().year) }
        assertTrue(files.reads.isEmpty())
    }

    @Test
    fun `a file is read once until it changes`() = runTest {
        val song = track(1)
        val files = FakeTagFiles(mapOf(song.ref to file("DATE" to listOf("2020"))))
        val source = FakeLibrary(listOf(song))
        val cache = FakeTrackTagCache()

        TagCompletedAudioLibrary(source, files, cache).observeTracks().test {
            awaitItem()
            awaitItem()
        }
        val restarted = TagCompletedAudioLibrary(source, files, cache)
        restarted.observeTracks().test {
            assertEquals(2020, awaitItem().single().year)

            files.files[song.ref] = file("DATE" to listOf("2021"))
            source.tracks.value = listOf(song.copy(dateModifiedEpochSeconds = 2))
            assertEquals(null, awaitItem().single().year)
            assertEquals(2021, awaitItem().single().year)
        }
        assertEquals(2, files.reads.size)
    }

    @Test
    fun `a song gone from the library is forgotten`() = runTest {
        val kept = track(1)
        val gone = track(2)
        val files =
            FakeTagFiles(
                mapOf(
                    kept.ref to file("DATE" to listOf("2020")),
                    gone.ref to file("DATE" to listOf("2020")),
                )
            )
        val source = FakeLibrary(listOf(kept, gone))
        val cache = FakeTrackTagCache()

        TagCompletedAudioLibrary(source, files, cache).observeTracks().test {
            awaitItem()
            awaitItem()
            source.tracks.value = listOf(kept)
            awaitItem()
        }
        assertEquals(setOf(kept.id), cache.entries.keys)
    }

    @Test
    fun `a song opened on its own is completed too`() = runTest {
        val song = track(1)
        val files = FakeTagFiles(mapOf(song.ref to file("DATE" to listOf("2020"))))
        val library =
            TagCompletedAudioLibrary(FakeLibrary(listOf(song)), files, FakeTrackTagCache())

        assertEquals(2020, library.track(song.id)?.year)
        assertEquals(2020, library.findByRef(song.ref)?.year)
        assertEquals(1, files.reads.size)
    }

    @Test
    fun `file tags are read the way the library lists them`() {
        val tags =
            TagMap.of(
                    "ARTIST" to listOf("Tiësto", "Tate McRae"),
                    "ALBUMARTIST" to listOf("Tiësto"),
                    "ALBUM" to listOf(" Drive "),
                    "YEAR" to listOf("1987"),
                    "TRACKNUMBER" to listOf("3/12"),
                    "DISCNUMBER" to listOf("0"),
                    "GENRE" to listOf(" "),
                )
                .toFileTrackTags()

        assertEquals(
            FileTrackTags(
                artist = "Tiësto, Tate McRae",
                album = "Drive",
                albumArtist = "Tiësto",
                year = 1987,
                trackNumber = 3,
                discNumber = null,
                genre = null,
            ),
            tags,
        )
    }
}
