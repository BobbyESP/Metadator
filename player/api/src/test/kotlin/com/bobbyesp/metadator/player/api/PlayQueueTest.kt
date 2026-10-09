/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.api

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayQueueTest {
    private fun track(id: Long) =
        Track(
            id = TrackId(id),
            ref = ContentRef("content://$id"),
            title = "Song $id",
            artist = null,
            album = null,
            albumArtist = null,
            albumId = 1,
            durationMs = 1000,
            trackNumber = null,
            discNumber = null,
            year = null,
            genre = null,
            mimeType = null,
            sizeBytes = 10,
            displayName = "$id.flac",
            folder = null,
            dateAddedEpochSeconds = 1,
            dateModifiedEpochSeconds = 1,
            artworkRef = null,
        )

    private val tracks = (0L until 20L).map(::track)

    @Test
    fun `plays in the order the songs came in`() {
        val queue = PlayQueue.of(tracks)
        assertEquals(tracks, queue.tracks)
        assertFalse(queue.isShuffled)
    }

    @Test
    fun `shuffling keeps every song once, in another order`() {
        val queue = PlayQueue.of(tracks).shuffled(random = Random(1))
        assertTrue(queue.isShuffled)
        assertNotEquals(tracks, queue.tracks)
        assertEquals(tracks, queue.tracks.sortedBy { it.id.value })
    }

    @Test
    fun `shuffling keeps the song that plays first`() {
        val queue = PlayQueue.of(tracks).shuffled(first = 7, random = Random(1))
        assertEquals(tracks[7], queue.tracks.first())
        assertEquals(tracks, queue.tracks.sortedBy { it.id.value })
    }

    @Test
    fun `shuffling again keeps the song of the shuffled order`() {
        val once = PlayQueue.of(tracks).shuffled(random = Random(1))
        val twice = once.shuffled(first = 5, random = Random(2))
        assertEquals(once.tracks[5], twice.tracks.first())
    }

    @Test
    fun `unshuffling goes back to the order the songs came in`() {
        val shuffled = PlayQueue.of(tracks).shuffled(random = Random(1))
        val playing = shuffled.tracks[3]
        val queue = shuffled.unshuffled()
        assertEquals(tracks, queue.tracks)
        assertFalse(queue.isShuffled)
        assertEquals(playing, queue.tracks[shuffled.sourceIndex(3)])
    }

    @Test
    fun `the same song twice is two entries`() {
        val twice = listOf(track(1), track(1), track(2))
        val queue = PlayQueue.of(twice).shuffled(first = 1, random = Random(1))
        assertEquals(3, queue.tracks.size)
        assertEquals(2, queue.tracks.count { it.id == TrackId(1) })
    }

    @Test
    fun `an empty queue shuffles to an empty queue`() {
        assertTrue(PlayQueue.Empty.shuffled(first = 0).tracks.isEmpty())
    }
}
