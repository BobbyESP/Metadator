/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import kotlin.random.Random

/**
 * What a queue plays, in the order it plays. Shuffling draws one new order and keeps it, so what
 * the queue shows is what plays next; the order the items came in is kept to go back to.
 */
internal class PlayQueue<T>
private constructor(private val source: List<T>, private val order: List<Int>) {
    val items: List<T> = order.map(source::get)

    /** A random order. The item at [first], when there is one, stays the first to play. */
    fun shuffled(first: Int? = null, random: Random = Random): PlayQueue<T> {
        val kept = first?.let(order::getOrNull)
        val rest = if (kept == null) source.indices.toList() else source.indices - kept
        return PlayQueue(source, listOfNotNull(kept) + rest.shuffled(random))
    }

    /** The order the items came in. */
    fun unshuffled(): PlayQueue<T> = PlayQueue(source, source.indices.toList())

    /** Where the item at [index] is in the order the items came in. */
    fun sourceIndex(index: Int): Int = order[index]

    companion object {
        fun <T> of(items: List<T>): PlayQueue<T> = PlayQueue(items, items.indices.toList())
    }
}
