/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.library

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.SortOrder
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.core.model.TrackSort
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryQueriesTest {
    private fun track(
        id: Long,
        title: String,
        artist: String? = "Artist",
        album: String? = "Album",
        year: Int? = 2000,
        track: Int? = id.toInt(),
        name: String = "$title.mp3",
    ) =
        Track(
            id = TrackId(id),
            ref = ContentRef("content://$id"),
            title = title,
            artist = artist,
            album = album,
            albumArtist = artist,
            albumId = 1,
            durationMs = id * 1000,
            trackNumber = track,
            discNumber = null,
            year = year,
            genre = "Rock",
            mimeType = "audio/mpeg",
            sizeBytes = 1,
            displayName = name,
            folder = "Music/$album/",
            dateAddedEpochSeconds = id,
            dateModifiedEpochSeconds = id,
            artworkRef = null,
        )

    private val tracks =
        listOf(
            track(1, "Come Together", artist = "The Beatles", album = "Abbey Road"),
            track(2, "Halo", artist = "Beyoncé", album = null, year = null),
            track(3, "Ünder Pressure", artist = "Queen", name = "under.flac"),
        )

    @Test
    fun `every search term has to match some field`() {
        assertEquals(
            listOf(1L),
            tracks.filtered(LibraryFilter(search = "beatles abbey")).map { it.id.value },
        )
        assertEquals(
            listOf(2L),
            tracks.filtered(LibraryFilter(search = "beyonce")).map { it.id.value },
        )
        assertEquals(
            emptyList<Long>(),
            tracks.filtered(LibraryFilter(search = "beatles queen")).map { it.id.value },
        )
    }

    @Test
    fun `needs attention finds songs missing essentials`() {
        assertEquals(
            listOf(2L),
            tracks.filtered(LibraryFilter(needsAttention = true)).map { it.id.value },
        )
    }

    @Test
    fun `format filter uses the extension`() {
        assertEquals(
            listOf(3L),
            tracks.filtered(LibraryFilter(formats = setOf("flac"))).map { it.id.value },
        )
    }

    @Test
    fun `sorting ignores accents and puts missing values last`() {
        val byTitle = tracks.sorted(SortOrder(TrackSort.Title), Locale.ENGLISH).map { it.title }
        assertEquals(listOf("Come Together", "Halo", "Ünder Pressure"), byTitle)
        val byAlbum = tracks.sorted(SortOrder(TrackSort.Album, ascending = false), Locale.ENGLISH)
        assertEquals(2L, byAlbum.last().id.value)
    }

    @Test
    fun `albums group by album artist and title`() {
        val albums = tracks.albums()
        assertEquals(listOf("Abbey Road", "Album"), albums.map { it.title })
    }

    @Test
    fun `an untagged folder album shared by several artists gives each artist its own key`() {
        val download =
            listOf(
                track(1, "One", artist = "Artist A", album = "Download"),
                track(2, "Two", artist = "Artist B", album = "Download"),
                track(3, "Three", artist = "Artist A", album = "download"),
            )
        val albums = download.albums()
        assertEquals(2, albums.size)
        assertEquals(albums.size, albums.map { it.key }.distinct().size)
    }

    @Test
    fun `a separator inside a name cannot make two albums share a key`() {
        val albums =
            listOf(
                    track(1, "One", artist = "A:", album = "B"),
                    track(2, "Two", artist = "A", album = ":B"),
                )
                .albums()
        assertEquals(2, albums.size)
        assertEquals(albums.size, albums.map { it.key }.distinct().size)
    }
}
