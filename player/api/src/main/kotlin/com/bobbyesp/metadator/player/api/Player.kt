/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.api

import com.bobbyesp.metadator.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * A simple music player: a queue, play and pause, seeking, skipping, shuffle and repeat. It keeps
 * playing in the background, with the system's media notification.
 */
interface PlayerController {
    val state: StateFlow<PlaybackState>

    /** Replaces the queue with [tracks] and starts at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false)

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun skipToNext()

    fun skipToPrevious()

    fun skipToQueueItem(index: Int)

    fun setShuffle(enabled: Boolean)

    fun setRepeatMode(mode: RepeatMode)

    /** Stops and clears the queue. */
    fun stop()
}

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val error: String? = null,
) {
    val current: Track?
        get() = queue.getOrNull(currentIndex)

    val isActive: Boolean
        get() = current != null

    val hasNext: Boolean
        get() = currentIndex < queue.lastIndex || repeatMode != RepeatMode.Off

    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

enum class RepeatMode {
    Off,
    All,
    One;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}
