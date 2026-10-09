/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.bobbyesp.metadator.player.api.PlaybackState
import kotlin.math.abs
import kotlin.math.min

/**
 * The position in the song on every frame. The player reports it twice a second, which is plenty
 * for a progress bar and far too coarse for lyrics that follow each word: between two reports this
 * counts the time itself, and each report corrects it.
 *
 * Read [positionMs] while drawing. Read in composition, it recomposes on every frame.
 */
@Stable
internal class PlaybackClock(initialMs: Long) {
    var positionMs by mutableLongStateOf(initialMs)
        private set

    private var exactMs = initialMs.toDouble()

    /** How far off the last report found the clock, still to be made up. */
    private var driftMs = 0.0

    /** The player says the song is at [reportedMs]. */
    fun report(reportedMs: Long, running: Boolean) {
        val drift = reportedMs - exactMs
        // A small difference is the clock and the player disagreeing, and is made up gradually:
        // jumping to it would make the lyrics tremble. A large one is a seek or another song.
        if (running && abs(drift) < SNAP_MS) {
            driftMs = drift
        } else {
            driftMs = 0.0
            exactMs = reportedMs.toDouble()
            positionMs = reportedMs
        }
    }

    /** A frame came [elapsedMs] after the last one, with the song playing. */
    fun advance(elapsedMs: Double) {
        val correction = driftMs * min(1.0, elapsedMs / BLEND_MS)
        driftMs -= correction
        exactMs += elapsedMs + correction
        positionMs = exactMs.toLong()
    }

    private companion object {
        const val SNAP_MS = 400.0
        const val BLEND_MS = 250.0
    }
}

/**
 * A clock following [playback], whose position is what [positionMs] reports. It asks for a frame on
 * every frame while the song plays, so it belongs only where something on screen moves with the
 * song.
 */
@Composable
internal fun rememberPlaybackClock(
    playback: PlaybackState,
    positionMs: () -> Long,
): PlaybackClock {
    val clock = remember { PlaybackClock(positionMs()) }
    val running = playback.isPlaying && !playback.isBuffering
    LaunchedEffect(clock, running, playback.current?.id) {
        snapshotFlow(positionMs).collect { clock.report(it, running) }
    }
    LaunchedEffect(clock, running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            clock.advance((now - last) / NANOS_PER_MS)
            last = now
        }
    }
    return clock
}

private const val NANOS_PER_MS = 1_000_000.0
