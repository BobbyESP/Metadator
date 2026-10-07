/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/** Every transition between destinations, in one place. Screens contribute nothing. */
object NavigationMotion {
    private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val Travel: FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = 300, easing = Emphasized)

    /** Forward: the new screen slides in from the end, the old one moves a little and fades. */
    fun forward(): ContentTransform =
        (slideInHorizontally(Travel) { it } + fadeIn(tween(150))) togetherWith
            (slideOutHorizontally(Travel) { -it / 4 } + fadeOut(tween(300)))

    fun backward(): ContentTransform =
        (slideInHorizontally(Travel) { -it / 4 } + fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(Travel) { it } + fadeOut(tween(150, delayMillis = 150)))

    /** The gesture shrinks the leaving screen, as the system's predictive back does. */
    fun predictiveBack(): ContentTransform =
        fadeIn(tween(300)) togetherWith (scaleOut(targetScale = 0.9f) + fadeOut(tween(300)))
}

val LocalNavSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Whether the destination has finished arriving, to start work that would stutter its motion. */
@Composable
fun isDestinationSettled(): Boolean {
    if (LocalNavSharedTransitionScope.current == null) return true
    return !LocalNavAnimatedContentScope.current.transition.isRunning
}

/** Shares an element (a cover) between two destinations; a no-op outside a navigation display. */
@Composable
fun Modifier.sharedElementAcrossDestinations(key: Any): Modifier {
    val scope = LocalNavSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    return with(scope) {
        this@sharedElementAcrossDestinations.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}
