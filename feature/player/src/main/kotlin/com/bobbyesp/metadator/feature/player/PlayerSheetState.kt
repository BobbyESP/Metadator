/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** The two shapes of the player: the bar floating over the library, and the whole screen. */
internal enum class PlayerSheetValue(val progress: Float) {
    Collapsed(0f),
    Expanded(1f);

    val other: PlayerSheetValue
        get() = if (this == Collapsed) Expanded else Collapsed
}

/**
 * Where the player is between its bar and the full screen, and what moves it there: a tap, a drag,
 * or the back gesture.
 *
 * Two things, one following the other:
 * - [progress], 0 for the bar and 1 for the screen, is the truth. A finger sets it directly; a
 *   release hands it to a spring, with the finger's velocity. Being a spring it runs past the end
 *   and comes back, and that excess is [overshoot]: the bounce.
 * - [transition] is the shared-element transition, which only ever goes from one state to the other
 *   and cannot go past either. [sync] keeps it seeked to wherever [progress] is.
 */
@Stable
internal class PlayerSheetState(
    initial: PlayerSheetValue,
    private val scope: CoroutineScope,
    /** How the sheet finishes on its own once released. */
    private val settleSpec: AnimationSpec<Float>,
    /** Above this speed a release follows the fling, whatever the distance covered. */
    private val velocityThreshold: Float,
) {
    val transition = SeekableTransitionState(initial)

    private val progress = Animatable(initial.progress)

    /** The distance, in pixels, a finger covers to take the sheet from one state to the other. */
    var travel by mutableStateOf(1f)

    private var dragging by mutableStateOf(false)
    private var dragFrom = initial

    /** Where the finger has it, kept apart from [progress] because that one is set a frame late. */
    private var dragPosition = initial.progress

    /** Whether the full player is on screen, or on its way there. */
    val isExpanded: Boolean
        get() =
            transition.currentState == PlayerSheetValue.Expanded ||
                progress.targetValue == PlayerSheetValue.Expanded.progress

    /** How far open the sheet is, from 0 (the bar) to 1 (the screen). Read it while drawing. */
    val fraction: Float
        get() = progress.value.coerceIn(0f, 1f)

    /** Whether the full player is on screen and at rest: not arriving, leaving or being dragged. */
    val isSettledExpanded: Boolean
        get() =
            transition.currentState == PlayerSheetValue.Expanded &&
                transition.targetState == PlayerSheetValue.Expanded

    /**
     * How far the spring is past where it is going: positive beyond the full screen, negative
     * beyond the bar, zero the rest of the time. Read it while drawing.
     */
    val overshoot: Float
        get() = progress.value - progress.value.coerceIn(0f, 1f)

    fun expand() = animateTo(PlayerSheetValue.Expanded)

    fun collapse() = animateTo(PlayerSheetValue.Collapsed)

    /** Collapses without a transition, for when what the sheet shows is gone. */
    fun snapToCollapsed() {
        dragging = false
        scope.launch { progress.snapTo(PlayerSheetValue.Collapsed.progress) }
    }

    /** Shows how far a back gesture has got, [backProgress] from 0 to 1, without committing. */
    fun previewCollapse(backProgress: Float) {
        if (transition.currentState != PlayerSheetValue.Expanded) return
        scope.launch { progress.snapTo(1f - backProgress * BACK_PREVIEW_FRACTION) }
    }

    /**
     * Moves the sheet with a finger that travelled [delta] pixels (negative is up). Returns how
     * much of it was used, for a list that shares the gesture.
     */
    fun dragBy(delta: Float): Float {
        if (!dragging) {
            dragging = true
            dragFrom = transition.currentState
            dragPosition = progress.value.coerceIn(0f, 1f)
        }
        val before = dragPosition
        dragPosition = (before - delta / travel).coerceIn(0f, 1f)
        val position = dragPosition
        scope.launch { progress.snapTo(position) }
        return (before - position) * travel
    }

    /** The finger left, at [velocity] pixels per second: finish the way it was going. */
    fun settle(velocity: Float) {
        if (!dragging) return
        dragging = false
        val upwards = -velocity
        val current = dragPosition
        val target =
            when {
                upwards > velocityThreshold -> PlayerSheetValue.Expanded
                upwards < -velocityThreshold -> PlayerSheetValue.Collapsed
                abs(current - dragFrom.progress) > COMMIT_FRACTION -> dragFrom.other
                else -> dragFrom
            }
        animateTo(target, initialVelocity = upwards / travel)
    }

    private fun animateTo(target: PlayerSheetValue, initialVelocity: Float = 0f) {
        scope.launch { progress.animateTo(target.progress, settleSpec, initialVelocity) }
    }

    /**
     * Keeps [transition] where [progress] is, for as long as the sheet is on screen. The transition
     * is told it has arrived only once the spring has come to rest, so that the two contents stay
     * composed while it is still bouncing.
     */
    suspend fun sync() {
        snapshotFlow { progress.value.coerceIn(0f, 1f) to (dragging || progress.isRunning) }
            .collectLatest { (position, moving) ->
                val from = transition.currentState
                when {
                    position == from.progress ->
                        if (transition.targetState != from) transition.snapTo(from)
                    position == from.other.progress && !moving -> transition.snapTo(from.other)
                    else -> transition.seekTo(abs(position - from.progress), from.other)
                }
            }
    }

    /**
     * Lets the full player's list hand its gesture over: once the list is at its top, pulling
     * further down pulls the player down, as a bottom sheet does.
     */
    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // The sheet gives back what it took before the list scrolls again.
                if (!dragging || source != NestedScrollSource.UserInput) return Offset.Zero
                return Offset(0f, dragBy(available.y))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val canStart = transition.currentState == PlayerSheetValue.Expanded
                if (source != NestedScrollSource.UserInput || available.y <= 0f || !canStart) {
                    return Offset.Zero
                }
                return Offset(0f, dragBy(available.y))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging) return Velocity.Zero
                val moved = dragPosition != dragFrom.progress
                settle(available.y)
                return if (moved) available else Velocity.Zero
            }
        }

    private companion object {
        /** Past this much of the way, a slow release still goes through. */
        const val COMMIT_FRACTION = 0.4f

        /** A back gesture only hints at the collapse; completing it does the rest. */
        const val BACK_PREVIEW_FRACTION = 0.3f
    }
}
