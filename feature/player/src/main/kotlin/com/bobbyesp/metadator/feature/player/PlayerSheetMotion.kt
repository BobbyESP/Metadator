/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ArcMode
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The room the collapsed player takes at the bottom, for whatever must stay clear of it. */
object PlayerSheetDefaults {
    val CollapsedClearance: Dp = 112.dp
}

/**
 * The pace of the container inside the transition: linear, so that a drag halfway up is the
 * transition halfway through and the edge stays under the finger. It is a timeline to seek along;
 * its length means nothing, since a finger or [SettleSpring] sets the pace.
 */
internal fun <T> timeline(): FiniteAnimationSpec<T> = tween(TIMELINE_MS, easing = LinearEasing)

/** The pace of what the container carries: slow to leave, slow to arrive. */
internal fun <T> traveller(): FiniteAnimationSpec<T> = tween(TIMELINE_MS, easing = Carried)

/**
 * The pace of what the container carries, on a curve instead of a straight line: sideways first and
 * up late on the way to the screen, down first and sideways late on the way back, which is the same
 * path walked the other way.
 *
 * That way round and not the other because of the container: its top edge rises at a steady pace,
 * and what rose ahead of it would be cut by it. An arc that keeps low stays behind the edge.
 */
@OptIn(ExperimentalAnimationSpecApi::class)
internal fun arcTraveller(): FiniteAnimationSpec<Rect> =
    ArcAnimationSpec(mode = ArcMode.ArcBelow, durationMillis = TIMELINE_MS, easing = Carried)

/**
 * How the sheet finishes once released. Looser than the motion scheme's springs, which are tuned
 * for a button or a card: this one moves the whole screen, starts with the finger's velocity, and
 * is meant to run a little past the end and come back.
 */
internal val SettleSpring = spring<Float>(dampingRatio = 0.68f, stiffness = 240f)

/**
 * How the sheet opens from a tap on the bar. Softer than [SettleSpring], which finishes what a
 * finger had already started and at its speed: a tap starts from rest and covers the whole way.
 */
internal val OpenSpring = spring<Float>(dampingRatio = 0.76f, stiffness = 110f)

/**
 * Slow to leave and slow to arrive, but never far ahead of the container's linear pace: what
 * travels is headed for the top of the screen, and anything quicker than the container's edge would
 * be cut by it.
 */
private val Carried = Easing { t -> t + (t * t * (3f - 2f * t) - t) * 0.85f }

/** The container's corner: round early on the way to the bar, straighten late on the way out. */
internal val StraightenLate = CubicBezierEasing(0.7f, 0f, 1f, 1f)
internal val RoundEarly = CubicBezierEasing(0f, 0f, 0.3f, 1f)

internal const val TIMELINE_MS = 400
internal const val ABOVE_CONTAINER = 2f

/**
 * How far open the sheet is when what only the bar has is gone, and between which two points what
 * only the screen has comes into focus. The gap between them is the surface alone, changing color:
 * the two never show through each other.
 */
internal const val BAR_GONE_BY = 0.2f
internal const val SCREEN_FROM = 0.3f
internal const val SCREEN_BY = 0.8f

/** The cover's size in the full player while the music is paused. */
internal const val PAUSED_COVER_SCALE = 0.86f

/** How much the full screen grows per unit of overshoot, and how far down the bar dips. */
internal const val EXPANDED_SWELL = 0.35f
internal const val COLLAPSED_DIP = 0.12f

internal val BarCorner = 28.dp
internal val BarMaxWidth = 560.dp

/** The bar's shadow where no halo lifts it. */
internal val BarShadow = 6.dp
internal val BarArtworkCorner = 16.dp
internal val ScreenArtworkCorner = 28.dp
internal val FlingThreshold = 125.dp

/** A rounded shape whose corner is read as it is drawn, so that animating it recomposes nothing. */
@Stable
internal class AnimatedCornerShape(private val corner: State<Dp>) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = RoundedCornerShape(corner.value).createOutline(size, layoutDirection, density)
}
