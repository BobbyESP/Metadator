/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bobbyesp.metadator.core.designsystem.theme.dissolved

/**
 * The pace of one thing replacing another through focus: what leaves starts slowly and is quickest
 * as it goes, and what arrives does the opposite, quick at first and slowing to a stop. The one
 * arriving waits for the other to be on its way.
 *
 * Tweens, where the app otherwise animates with the theme's springs: a spring cannot start slowly
 * and end at its quickest, which is the whole of the way out. The curves are Material's emphasized
 * accelerate and decelerate.
 */
object ThroughFocus {
    fun <T> leaving(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(LEAVE_MS, delayMillis = delayMillis, easing = EmphasizedAccelerate)

    fun <T> arriving(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(ARRIVE_MS, delayMillis = ARRIVE_AFTER_MS + delayMillis, easing = EmphasizedDecelerate)

    /** The pace of one picture coming into focus over another, which only waits beneath it. */
    fun <T> replacing(): FiniteAnimationSpec<T> = tween(ARRIVE_MS, easing = EmphasizedDecelerate)

    private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private const val LEAVE_MS = 280
    private const val ARRIVE_MS = 440
    private const val ARRIVE_AFTER_MS = 160
}

/**
 * Something that turns up like a figure on a counter: it rolls in from under its place, out of
 * focus from the speed and sharp as it stops. It rolls out over the top the same way, and a new one
 * rolls the old one out. Nothing clips it on the way: it is faint enough there to pass over what is
 * next to it.
 *
 * @param target what to show, or `null` for nothing
 * @param delayMillis how much later than its neighbours this one moves, for several in a row
 */
@Composable
fun <T : Any> RolledIn(
    target: T?,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = target,
        transitionSpec = {
            // No fade of its own: going through focus is one.
            slideInVertically(ThroughFocus.arriving(delayMillis)) { it } togetherWith
                slideOutVertically(ThroughFocus.leaving(delayMillis)) { -it } using
                SizeTransform(clip = false)
        },
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
        label = "RolledIn",
    ) { shown ->
        val focus =
            transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) ThroughFocus.arriving(delayMillis)
                    else ThroughFocus.leaving(delayMillis)
                },
                label = "Focus",
            ) {
                if (it == EnterExitState.Visible) 1f else 0f
            }
        val presence = remember(focus) { { focus.value } }
        if (shown != null) {
            Box(Modifier.dissolved(presence)) { content(shown) }
        }
    }
}
