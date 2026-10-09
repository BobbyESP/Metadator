/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.api

import com.bobbyesp.metadator.core.model.Track
import kotlin.random.Random

/**
 * The songs to play, in the order they play. Shuffling draws one new order and keeps it, so what
 * the queue shows is what plays next; the order the songs came in is kept to go back to.
 */
class PlayQueue
private constructor(
    private val source: List<Track>,
    private val order: List<Int>,
    val isShuffled: Boolean,
) {
    val tracks: List<Track> = order.map(source::get)

    /** A random order. The song at [first], when there is one, stays the first to play. */
    fun shuffled(first: Int? = null, random: Random = Random): PlayQueue {
        val kept = first?.let(order::getOrNull)
        val rest = if (kept == null) source.indices.toList() else source.indices - kept
        return PlayQueue(source, listOfNotNull(kept) + rest.shuffled(random), isShuffled = true)
    }

    /** The order the songs came in. */
    fun unshuffled(): PlayQueue = PlayQueue(source, source.indices.toList(), isShuffled = false)

    /** Where the song at [index] is in the order the songs came in. */
    fun sourceIndex(index: Int): Int = order[index]

    companion object {
        val Empty = PlayQueue(emptyList(), emptyList(), isShuffled = false)

        fun of(tracks: List<Track>): PlayQueue =
            PlayQueue(tracks, tracks.indices.toList(), isShuffled = false)
    }
}
