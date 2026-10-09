/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayQueueTest {
    private val songs = (0 until 20).map { "Song $it" }

    @Test
    fun `plays in the order the songs came in`() {
        assertEquals(songs, PlayQueue.of(songs).items)
    }

    @Test
    fun `shuffling keeps every song once, in another order`() {
        val queue = PlayQueue.of(songs).shuffled(random = Random(1))
        assertNotEquals(songs, queue.items)
        assertEquals(songs.toSet(), queue.items.toSet())
        assertEquals(songs.size, queue.items.size)
    }

    @Test
    fun `shuffling keeps the song that plays first`() {
        val queue = PlayQueue.of(songs).shuffled(first = 7, random = Random(1))
        assertEquals(songs[7], queue.items.first())
        assertEquals(songs.toSet(), queue.items.toSet())
    }

    @Test
    fun `shuffling again keeps the song of the shuffled order`() {
        val once = PlayQueue.of(songs).shuffled(random = Random(1))
        val twice = once.shuffled(first = 5, random = Random(2))
        assertEquals(once.items[5], twice.items.first())
    }

    @Test
    fun `unshuffling goes back to the order the songs came in`() {
        val shuffled = PlayQueue.of(songs).shuffled(random = Random(1))
        val playing = shuffled.items[3]
        val queue = shuffled.unshuffled()
        assertEquals(songs, queue.items)
        assertEquals(playing, queue.items[shuffled.sourceIndex(3)])
    }

    @Test
    fun `the same song twice is two entries`() {
        val queue = PlayQueue.of(listOf("A", "A", "B")).shuffled(first = 1, random = Random(1))
        assertEquals(3, queue.items.size)
        assertEquals(2, queue.items.count { it == "A" })
    }

    @Test
    fun `an empty queue shuffles to an empty queue`() {
        assertTrue(PlayQueue.of(emptyList<String>()).shuffled(first = 0).items.isEmpty())
    }
}
