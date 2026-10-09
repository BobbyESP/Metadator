/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder

/**
 * Makes shuffle an order of the queue. ExoPlayer's shuffle mode keeps its random order to itself,
 * so the queue a controller reads would not be the one played. Here the mode plays the queue as it
 * stands, and turning it on or off puts the items themselves in a new order, around the one that
 * plays. Every controller sees the same queue: the app, the notification, a headset.
 */
@OptIn(UnstableApi::class)
internal class QueueShuffler(private val player: ExoPlayer) : Player.Listener {
    private var shuffled: PlayQueue<MediaItem>? = null

    init {
        player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
        player.addListener(this)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        val before = shuffled
        shuffled = null
        val count = player.mediaItemCount
        if (count == 0) return
        val current = player.currentMediaItemIndex
        if (shuffleModeEnabled) {
            val queue = PlayQueue.of(List(count, player::getMediaItemAt)).shuffled(first = current)
            shuffled = queue
            arrange(queue.items, current, 0)
        } else if (before != null && before.items.size == count) {
            arrange(before.unshuffled().items, current, before.sourceIndex(current))
        }
    }

    /** Puts the queue in the order of [items] without touching the item that plays. */
    private fun arrange(items: List<MediaItem>, from: Int, to: Int) {
        player.moveMediaItem(from, 0)
        player.replaceMediaItems(
            1,
            player.mediaItemCount,
            items.filterIndexed { index, _ -> index != to },
        )
        player.moveMediaItem(0, to)
    }
}
