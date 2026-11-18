/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * What the bar and the full player tie together, as the modifiers each applies to its own piece, so
 * that the two contents read as one thing that grows. See [PlayerSheet] for how it all fits.
 *
 * Built for one content of the sheet's `AnimatedContent`: the same pieces, from the point of view
 * of the bar or of the screen.
 */
@Stable
internal class SheetPieces(
    /** The surface: bounds are what animates, and both contents are clipped to it. */
    val container: Modifier,
    /** The surface's shape, its corner following the transition. */
    val shape: Shape,
    /** The cover: one picture, so only one of the two is drawn. */
    val artwork: Modifier,
    val artworkShape: Shape,
    val title: Modifier,
    val artist: Modifier,
    val playButton: Modifier,
    /** For what is on the bar's surface: laid out as a bar throughout, as it grows. */
    val barContent: Modifier,
    /** For what is on the full player's surface: laid out once, as it will be at rest. */
    val screenContent: Modifier,
)

/**
 * The pieces of [isBar]'s content, tied to the other's through [scope].
 *
 * What is inside either surface is told to ignore the surface's changing bounds
 * (`skipToLookaheadSize`, and for the screen `skipToLookaheadPosition` too): it is laid out once,
 * as it will be at rest, and the container clips it. Without that, everything inside is laid out
 * again every frame in bounds that are changing, and the shared elements in it start by jumping.
 *
 * @param pausedCoverScale how far the cover steps back while the music is paused, read while
 *   drawing
 */
@Composable
internal fun SharedTransitionScope.rememberSheetPieces(
    scope: AnimatedVisibilityScope,
    isBar: Boolean,
    sheet: PlayerSheetState,
    pausedCoverScale: State<Float>,
): SheetPieces {
    val arriving = scope.transition.targetState == EnterExitState.Visible

    val corner =
        scope.transition.animateDp(
            transitionSpec = {
                val becomingBar = (targetState == EnterExitState.Visible) == isBar
                tween(TIMELINE_MS, easing = if (becomingBar) RoundEarly else StraightenLate)
            },
            label = "Corner",
        ) {
            if ((it == EnterExitState.Visible) == isBar) BarCorner else 0.dp
        }
    val artworkCorner =
        scope.transition.animateDp(transitionSpec = { traveller() }, label = "Artwork") {
            if ((it == EnterExitState.Visible) == isBar) BarArtworkCorner else ScreenArtworkCorner
        }
    val shape = remember(corner) { AnimatedCornerShape(corner) }
    val artworkShape = remember(artworkCorner) { AnimatedCornerShape(artworkCorner) }
    val overlayClip = remember(shape) { OverlayClip(shape) }

    val container =
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(SharedKey.Container),
            animatedVisibilityScope = scope,
            enter =
                fadeIn(
                    tween(
                        durationMillis = TIMELINE_MS / 2,
                        delayMillis = if (isBar) TIMELINE_MS / 2 else 0,
                        easing = LinearEasing,
                    )
                ),
            exit = ExitTransition.None,
            boundsTransform = { _, _ -> timeline() },
            zIndexInOverlay = if (arriving) 1f else 0f,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = overlayClip,
        )

    // The cover steps back while the music is paused, in the full player only. Both covers do it by
    // as much as the sheet is open: a shared element draws one of the two from the first frame, and
    // if only the screen's stepped back, the cover would jump to its size there.
    val artwork =
        Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(SharedKey.Artwork),
                animatedVisibilityScope = scope,
                boundsTransform = { _, _ -> traveller() },
                zIndexInOverlay = ABOVE_CONTAINER,
            )
            .graphicsLayer {
                val scale = lerp(1f, pausedCoverScale.value, sheet.fraction)
                scaleX = scale
                scaleY = scale
            }

    val text =
        SharedTransitionScope.ResizeMode.scaleToBounds(
            contentScale = ContentScale.FillHeight,
            alignment = Alignment.CenterStart,
        )

    return SheetPieces(
        container = container,
        shape = shape,
        artwork = artwork,
        artworkShape = artworkShape,
        title = travellingText(SharedKey.Title, scope, text),
        artist = travellingText(SharedKey.Artist, scope, text),
        playButton =
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(SharedKey.PlayButton),
                animatedVisibilityScope = scope,
                boundsTransform = { _, _ -> arcTraveller() },
                zIndexInOverlay = ABOVE_CONTAINER,
            ),
        barContent = Modifier.skipToLookaheadSize(),
        screenContent =
            Modifier.skipToLookaheadSize()
                .skipToLookaheadPosition()
                .nestedScroll(sheet.nestedScrollConnection),
    )
}

/**
 * Ties a line of text the bar and the full player both have. It is a shared element in all but
 * name: only the version the transition is going to is drawn, the other gone from the first frame.
 * `sharedElement` itself lays its content out again in the bounds as they change, which would wrap
 * or cut a text differently at every width; these bounds scale it.
 *
 * The two say the same, so the line travels whole, and it is not clipped to its bounds on the way:
 * they travel on an arc, which moves their corners apart and together again, so they do not keep
 * the shape of the text and would cut into it.
 */
@Composable
private fun SharedTransitionScope.travellingText(
    key: SharedKey,
    scope: AnimatedVisibilityScope,
    resizeMode: SharedTransitionScope.ResizeMode,
): Modifier =
    Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key),
        animatedVisibilityScope = scope,
        enter = EnterTransition.None,
        exit = fadeOut(snap()),
        boundsTransform = { _, _ -> arcTraveller() },
        resizeMode = resizeMode,
        zIndexInOverlay = ABOVE_CONTAINER,
    )

private enum class SharedKey {
    Container,
    Artwork,
    Title,
    Artist,
    PlayButton,
}
