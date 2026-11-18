/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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
 * - [position], 0 for the bar and 1 for the screen, is the truth. A finger writes it directly, in
 *   the same frame; a release hands it to a spring, with the finger's velocity. Being a spring it
 *   runs past the end and comes back, and that excess is [overshoot]: the bounce.
 * - [transition] is the shared-element transition, which only ever goes from one state to the other
 *   and cannot go past either. [sync] keeps it seeked to wherever [position] is.
 *
 * Nothing here launches a coroutine per touch event: only a settle does, one at a time, and a new
 * touch or a new settle cancels the one running.
 */
@Stable
internal class PlayerSheetState(
    initial: PlayerSheetValue,
    private val scope: CoroutineScope,
    /** How the sheet finishes on its own once released. */
    private val settleSpec: AnimationSpec<Float>,
    /** How the sheet opens from a tap, with no finger to give it a pace. */
    private val openSpec: AnimationSpec<Float>,
    /** Above this speed a release follows the fling, whatever the distance covered. */
    private val velocityThreshold: Float,
) {
    val transition = SeekableTransitionState(initial)

    /** The distance, in pixels, a finger covers to take the sheet from one state to the other. */
    var travel = 1f

    private var position by mutableFloatStateOf(initial.progress)

    /** Where [position] is going: the end of a settle, or where the finger or a gesture left it. */
    private var heading by mutableFloatStateOf(initial.progress)

    private var dragging by mutableStateOf(false)
    private var settle: Job? = null
    private var settling by mutableStateOf(false)
    private var dragFrom = initial

    /** Whether the sheet was last sent to the bar: a back gesture's progress must not undo that. */
    private var collapsing = false

    /** Whether the full player is on screen, or on its way there. */
    val isExpanded: Boolean
        get() =
            transition.currentState == PlayerSheetValue.Expanded ||
                heading == PlayerSheetValue.Expanded.progress

    /** How far open the sheet is, from 0 (the bar) to 1 (the screen). Read it while drawing. */
    val fraction: Float
        get() = position.coerceIn(0f, 1f)

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
        get() = position - fraction

    fun expand() = animateTo(PlayerSheetValue.Expanded)

    /** Opens the full player from a tap on the bar: the whole way, unhurried. */
    fun open() = animateTo(PlayerSheetValue.Expanded, spec = openSpec)

    fun collapse() = animateTo(PlayerSheetValue.Collapsed)

    /** Collapses without a transition, for when what the sheet shows is gone. */
    fun snapToCollapsed() {
        cancelSettle()
        dragging = false
        jumpTo(PlayerSheetValue.Collapsed.progress)
    }

    /** Shows how far a back gesture has got, [backProgress] from 0 to 1, without committing. */
    fun previewCollapse(backProgress: Float) {
        if (collapsing || transition.currentState != PlayerSheetValue.Expanded) return
        cancelSettle()
        jumpTo(1f - backProgress * BACK_PREVIEW_FRACTION)
    }

    /**
     * Moves the sheet with a finger that travelled [delta] pixels (negative is up). Returns how
     * much of it was used, for a list that shares the gesture.
     */
    fun dragBy(delta: Float): Float {
        if (!dragging) startDrag()
        val before = position
        jumpTo((before - delta / travel).coerceIn(0f, 1f))
        return (before - position) * travel
    }

    /** The finger left, at [velocity] pixels per second: finish the way it was going. */
    fun settle(velocity: Float) {
        if (!dragging) return
        dragging = false
        val upwards = -velocity
        val target =
            when {
                upwards > velocityThreshold -> PlayerSheetValue.Expanded
                upwards < -velocityThreshold -> PlayerSheetValue.Collapsed
                abs(position - dragFrom.progress) > COMMIT_FRACTION -> dragFrom.other
                else -> dragFrom
            }
        animateTo(target, initialVelocity = upwards / travel)
    }

    /**
     * Keeps [transition] where [position] is, for as long as the sheet is on screen. The transition
     * is told it has arrived only once the spring has come to rest, so that the two contents stay
     * composed while it is still bouncing.
     */
    suspend fun sync() {
        snapshotFlow { fraction to (dragging || settling) }
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
                val moved = position != dragFrom.progress
                settle(available.y)
                return if (moved) available else Velocity.Zero
            }
        }

    private fun startDrag() {
        cancelSettle()
        dragging = true
        collapsing = false
        dragFrom = transition.currentState
        position = position.coerceIn(0f, 1f)
    }

    /** Puts the sheet at [to] now, where it is going as well. */
    private fun jumpTo(to: Float) {
        position = to
        heading = to
    }

    private fun cancelSettle() {
        settle?.cancel()
        settle = null
        settling = false
    }

    private fun animateTo(
        target: PlayerSheetValue,
        initialVelocity: Float = 0f,
        spec: AnimationSpec<Float> = settleSpec,
    ) {
        cancelSettle()
        collapsing = target == PlayerSheetValue.Collapsed
        heading = target.progress
        // Started only once it is recorded as the current one, so that its end, which may come in
        // the same frame, never clears the flag of a settle that has replaced it.
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    animate(position, target.progress, initialVelocity, spec) { value, _ ->
                        position = value
                    }
                } finally {
                    if (settle === coroutineContext[Job]) {
                        settle = null
                        settling = false
                    }
                }
            }
        settle = job
        settling = true
        job.start()
    }

    private companion object {
        /** Past this much of the way, a slow release still goes through. */
        const val COMMIT_FRACTION = 0.4f

        /** A back gesture only hints at the collapse; completing it does the rest. */
        const val BACK_PREVIEW_FRACTION = 0.3f
    }
}
