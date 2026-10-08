/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent

/** Every transition between destinations, in one place. Screens contribute nothing. */
object NavigationMotion {
    private const val BEHIND_SCALE = 0.94f
    private const val HELD_SCALE = 0.9f
    private const val GESTURE_MS = 250
    private val Peek = CubicBezierEasing(0f, 0f, 0f, 1f)
    private val Leave = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Forward: the new screen springs in from the end, over the old one as it steps back. */
    fun AnimatedContentTransitionScope<*>.forward(motion: MotionScheme): ContentTransform {
        val travel = motion.defaultSpatialSpec<IntOffset>().toThePixel()
        val arriving =
            slideIntoContainer(SlideDirection.Start, travel) + fadeIn(motion.fastEffectsSpec())
        val steppingBack =
            slideOutOfContainer(SlideDirection.Start, travel) { it / 4 } +
                scaleOut(motion.defaultSpatialSpec(), targetScale = BEHIND_SCALE) +
                fadeOut(motion.defaultEffectsSpec())
        return arriving togetherWith steppingBack
    }

    /** Backward: the screen leaves to where it came from, and the one behind steps up. */
    fun AnimatedContentTransitionScope<*>.backward(motion: MotionScheme): ContentTransform {
        val travel = motion.defaultSpatialSpec<IntOffset>().toThePixel()
        val steppingUp =
            slideIntoContainer(SlideDirection.End, travel) { it / 4 } +
                scaleIn(motion.defaultSpatialSpec(), initialScale = BEHIND_SCALE) +
                fadeIn(motion.fastEffectsSpec())
        val leaving = slideOutOfContainer(SlideDirection.End, travel)
        return steppingUp togetherWith leaving
    }

    /**
     * The back gesture: the screen shrinks under the finger, showing the one behind, then leaves
     * the way the finger goes.
     *
     * Tweens of one length, not the theme's springs: the display seeks this transition by the
     * gesture's progress, and each spring would run through its own time under the same finger.
     */
    fun predictiveBack(@NavigationEvent.SwipeEdge swipeEdge: Int): ContentTransform {
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        return (scaleIn(tween(GESTURE_MS, easing = Peek), initialScale = BEHIND_SCALE) +
            fadeIn(tween(GESTURE_MS / 3, easing = LinearEasing))) togetherWith
            (scaleOut(tween(GESTURE_MS, easing = Peek), targetScale = HELD_SCALE) +
                slideOutHorizontally(tween(GESTURE_MS, easing = Leave)) { direction * it })
    }

    /**
     * A spring with no threshold settles to a hundredth of a pixel, and the transition, with both
     * screens composed, runs on long after anything moves.
     */
    private fun FiniteAnimationSpec<IntOffset>.toThePixel(): FiniteAnimationSpec<IntOffset> =
        if (this is SpringSpec) spring(dampingRatio, stiffness, IntOffset.VisibilityThreshold)
        else this
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
