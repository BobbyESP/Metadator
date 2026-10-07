/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorAccentTheme
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.player.api.PlayerController
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

object PlayerSheetDefaults {
    /** The room the collapsed player takes at the bottom, for whatever must stay clear of it. */
    val CollapsedClearance: Dp = 112.dp
}

/**
 * The player, over everything else: a bar while browsing, the whole screen when opened. Both are
 * one surface. Tapping the bar opens it; so does dragging it up, and then the bar's edges follow
 * the finger out to the edges of the screen, its corners straightening on the way.
 *
 * It is a shared element transition between the two contents of an `AnimatedContent`:
 * - The bar's surface and the screen's are one `sharedBounds`: the container. Its bounds are what
 *   animates. What is inside either is told to ignore that (`skipToLookaheadSize`, and for the
 *   screen `skipToLookaheadPosition` too): it is laid out once, as it will be at rest, and the
 *   container clips it. Without that, everything inside is laid out again every frame in bounds
 *   that are changing, and the shared elements in it start by jumping.
 * - The cover is a `sharedElement`: the same picture in both, so only one of them is drawn.
 * - The song's name and the play button are `sharedBounds`: they differ between the two, and one
 *   fades into the other while scaling.
 *
 * The transition is seekable, which is what lets a drag hold it halfway; [PlayerSheetState] is what
 * moves it.
 *
 * The whole sheet, bar included, takes its colors from the cover of the song: it is one surface,
 * and a color that differed between its two sizes would change halfway through the transition.
 *
 * @param visible whether the player belongs on the screen under it; the bar hides where it doesn't
 * @param onEdit asked to open the editor for the playing song, by its URI
 * @param layout how the full player is laid out, which the shell decides from the window
 * @param expansion told how far open the sheet is, for the shell to step back what is behind it
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerSheet(
    visible: Boolean,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    layout: PlayerLayout = PlayerLayout.Stacked,
    expansion: PlayerSheetExpansion? = null,
) {
    val player: PlayerController = koinInject()
    val playback by player.state.collectAsStateWithLifecycle()
    val track = playback.current
    RequestNotificationsOnFirstPlay(active = playback.isActive)

    val sheet = rememberPlayerSheetState()
    val available = visible && track != null
    LaunchedEffect(available) { if (!available) sheet.snapToCollapsed() }
    LaunchedEffect(sheet) { sheet.sync() }
    CollapseOnBack(sheet)
    DisposableEffect(sheet, expansion) {
        expansion?.source = { sheet.fraction }
        onDispose { expansion?.source = null }
    }

    val viewModel: NowPlayingViewModel = koinViewModel()
    val nowPlaying by viewModel.state.collectAsStateWithLifecycle()
    // On every new song, and each time the player is opened: the song's tags may have been edited
    // since they were read, and the editor is one tap away from here.
    LaunchedEffect(track?.ref, sheet.isExpanded) {
        if (track != null) viewModel.onIntent(NowPlayingIntent.Show(track))
    }

    val motion = MaterialTheme.motionScheme
    val dragState = rememberDraggableState { delta -> sheet.dragBy(delta) }
    val dragToMove =
        Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> sheet.settle(velocity) },
        )

    MetadatorAccentTheme(accent = nowPlaying.accent?.let(::Color)) {
        SharedTransitionLayout(
            modifier =
                modifier
                    .fillMaxSize()
                    .onSizeChanged { sheet.travel = it.height.toFloat() }
                    // The bounce. The transition stops dead at either end, so what the spring does
                    // beyond them is shown on everything at once: the screen swells a little past
                    // full, the bar dips a little past its place.
                    .graphicsLayer {
                        val overshoot = sheet.overshoot
                        if (overshoot > 0f) {
                            val scale = 1f + overshoot * EXPANDED_SWELL
                            scaleX = scale
                            scaleY = scale
                        } else if (overshoot < 0f) {
                            translationY = -overshoot * size.height * COLLAPSED_DIP
                        }
                    }
        ) {
            val transition = rememberTransition(sheet.transition, label = "PlayerSheet")
            transition.AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                // Nothing of its own: every pixel that moves is a shared element, and the content
                // that is leaving has to stay until they have arrived.
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished
                },
                contentAlignment = Alignment.BottomCenter,
            ) { value ->
                val scope = this
                val isBar = value == PlayerSheetValue.Collapsed
                // Whether this content is the one arriving. It is drawn over the one leaving, which
                // stays solid underneath, so the surface is never see-through.
                val arriving = scope.transition.targetState == EnterExitState.Visible
                // Shapes are not animated by shared elements; a value of the same transition is.
                // Each
                // content asks what the sheet as a whole looks like at this point, whichever of the
                // two it is: round while a bar, square once the screen.
                val corner by
                    scope.transition.animateDp(
                        transitionSpec = {
                            val becomingBar = (targetState == EnterExitState.Visible) == isBar
                            tween(
                                TIMELINE_MS,
                                easing = if (becomingBar) RoundEarly else StraightenLate,
                            )
                        },
                        label = "Corner",
                    ) {
                        if ((it == EnterExitState.Visible) == isBar) BarCorner else 0.dp
                    }
                val artworkCorner by
                    scope.transition.animateDp(
                        transitionSpec = { traveller() },
                        label = "Artwork",
                    ) {
                        if ((it == EnterExitState.Visible) == isBar) BarArtworkCorner
                        else ScreenArtworkCorner
                    }
                val shape = RoundedCornerShape(corner)
                // What only the bar has is out of the way early going up, and back late coming
                // down.
                val barContentAlpha by
                    scope.transition.animateFloat(
                        transitionSpec = {
                            val part = TIMELINE_MS / 4
                            if (targetState == EnterExitState.Visible) {
                                tween(part, delayMillis = TIMELINE_MS - part, easing = LinearEasing)
                            } else {
                                tween(part, easing = LinearEasing)
                            }
                        },
                        label = "BarContentAlpha",
                    ) {
                        if (it == EnterExitState.Visible) 1f else 0f
                    }

                val container =
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(SharedKey.Container),
                        animatedVisibilityScope = scope,
                        // The screen shows up during the first half of the way up and holds through
                        // the first half of the way down; the bar takes the other half.
                        enter =
                            fadeIn(
                                tween(
                                    durationMillis = TIMELINE_MS / 2,
                                    delayMillis = if (isBar) TIMELINE_MS / 2 else 0,
                                    easing = LinearEasing,
                                )
                            ),
                        exit = ExitTransition.None,
                        // Linear: the surface's edge is what the finger holds.
                        boundsTransform = { _, _ -> timeline() },
                        zIndexInOverlay = if (arriving) 1f else 0f,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        // Also the clip of every shared element inside, which is their default.
                        clipInOverlayDuringTransition = OverlayClip(shape),
                    )
                // The same picture in both: a shared element, of which only the arriving one is
                // drawn.
                val artwork =
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(SharedKey.Artwork),
                        animatedVisibilityScope = scope,
                        boundsTransform = { _, _ -> traveller() },
                        zIndexInOverlay = ABOVE_CONTAINER,
                    )
                // Different in each: shared bounds, scaled rather than laid out again (text would
                // wrap differently at every width), one handing over to the other halfway.
                val title =
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(SharedKey.Title),
                        animatedVisibilityScope = scope,
                        enter = handOverIn(),
                        exit = handOverOut(),
                        boundsTransform = { _, _ -> traveller() },
                        zIndexInOverlay = ABOVE_CONTAINER,
                    )
                val playButton =
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(SharedKey.PlayButton),
                        animatedVisibilityScope = scope,
                        enter = handOverIn(),
                        exit = handOverOut(),
                        boundsTransform = { _, _ -> traveller() },
                        zIndexInOverlay = ABOVE_CONTAINER,
                    )

                when (value) {
                    PlayerSheetValue.Collapsed ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            AnimatedVisibility(
                                visible = available,
                                // A spring, so the bar lands with a little bounce.
                                enter =
                                    slideInVertically(motion.defaultSpatialSpec()) { it * 2 } +
                                        fadeIn(motion.defaultEffectsSpec()),
                                exit =
                                    slideOutVertically(motion.fastSpatialSpec()) { it * 2 } +
                                        fadeOut(motion.fastEffectsSpec()),
                                modifier =
                                    Modifier.navigationBarsPadding()
                                        .padding(bottom = Spacing.medium),
                            ) {
                                if (track != null) {
                                    MiniPlayerBar(
                                        track = track,
                                        playback = playback,
                                        player = player,
                                        onOpen = sheet::expand,
                                        shape = shape,
                                        artworkShape = RoundedCornerShape(artworkCorner),
                                        modifier = container.then(dragToMove),
                                        // Laid out as a bar throughout, at the top of the surface
                                        // as
                                        // it grows.
                                        contentModifier = Modifier.skipToLookaheadSize(),
                                        artworkModifier = artwork,
                                        titleModifier = title,
                                        playButtonModifier = playButton,
                                        contentAlpha = { barContentAlpha },
                                    )
                                }
                            }
                        }
                    PlayerSheetValue.Expanded -> {
                        // Laid out for the whole screen from the start and kept where it will be:
                        // the container's bounds open over it like a window.
                        val content =
                            container
                                .skipToLookaheadSize()
                                .skipToLookaheadPosition()
                                .nestedScroll(sheet.nestedScrollConnection)
                        val edit = { uri: String ->
                            sheet.collapse()
                            onEdit(uri)
                        }
                        if (track == null || layout == PlayerLayout.Stacked) {
                            NowPlayingContent(
                                playback = playback,
                                player = player,
                                onClose = sheet::collapse,
                                onEdit = edit,
                                modifier = content,
                                topBarModifier = dragToMove,
                                artworkShape = RoundedCornerShape(artworkCorner),
                                artworkModifier = artwork,
                                titleModifier = title,
                                playButtonModifier = playButton,
                            )
                        } else {
                            NowPlayingWideContent(
                                track = track,
                                playback = playback,
                                player = player,
                                // Not the last song's, for the moment before this one's are asked
                                // for.
                                lyrics = nowPlaying.lyrics.takeIf { nowPlaying.ref == track.ref },
                                compact = layout == PlayerLayout.SideBySideCompact,
                                settled = sheet.isSettledExpanded,
                                onClose = sheet::collapse,
                                onEdit = edit,
                                modifier = content,
                                topBarModifier = dragToMove,
                                artworkShape = RoundedCornerShape(artworkCorner),
                                artworkModifier = artwork,
                                titleModifier = title,
                                playButtonModifier = playButton,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPlayerSheetState(): PlayerSheetState {
    val scope = rememberCoroutineScope()
    val velocityThreshold = with(LocalDensity.current) { FlingThreshold.toPx() }
    // Only which end it rests at is worth keeping across a rotation.
    var expanded by rememberSaveable { mutableStateOf(false) }
    val sheet = remember {
        PlayerSheetState(
            initial = if (expanded) PlayerSheetValue.Expanded else PlayerSheetValue.Collapsed,
            scope = scope,
            settleSpec = SettleSpring,
            velocityThreshold = velocityThreshold,
        )
    }
    LaunchedEffect(sheet) {
        snapshotFlow { sheet.transition.currentState }
            .collect { expanded = it == PlayerSheetValue.Expanded }
    }
    return sheet
}

/** Back closes the full player, and the gesture shows it starting to before it is let go. */
@Composable
private fun CollapseOnBack(sheet: PlayerSheetState) {
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val progress =
        (backState.transitionState as? NavigationEventTransitionState.InProgress)
            ?.latestEvent
            ?.progress
    LaunchedEffect(progress) { if (progress != null) sheet.previewCollapse(progress) }
    NavigationBackHandler(
        state = backState,
        isBackEnabled = sheet.isExpanded,
        onBackCancelled = sheet::expand,
        onBackCompleted = sheet::collapse,
    )
}

/**
 * The pace of the container inside the transition: linear, so that a drag halfway up is the
 * transition halfway through and the edge stays under the finger. It is a timeline to seek along;
 * its length means nothing, since a finger or [SettleSpring] sets the pace.
 */
private fun <T> timeline(): FiniteAnimationSpec<T> = tween(TIMELINE_MS, easing = LinearEasing)

/** The pace of what the container carries: slow to leave, slow to arrive. */
private fun <T> traveller(): FiniteAnimationSpec<T> = tween(TIMELINE_MS, easing = Carried)

/** Two different things in the same bounds: one leaves in the first half, the other comes after. */
private fun handOverOut(): ExitTransition = fadeOut(tween(TIMELINE_MS / 2, easing = LinearEasing))

private fun handOverIn(): EnterTransition =
    fadeIn(tween(TIMELINE_MS / 2, delayMillis = TIMELINE_MS / 2, easing = LinearEasing))

/**
 * How the sheet finishes once released. Looser than the motion scheme's springs, which are tuned
 * for a button or a card: this one moves the whole screen, starts with the finger's velocity, and
 * is meant to run a little past the end and come back.
 */
private val SettleSpring = spring<Float>(dampingRatio = 0.68f, stiffness = 240f)

/**
 * Slow to leave and slow to arrive, but never far ahead of the container's linear pace: what
 * travels is headed for the top of the screen, and anything quicker than the container's edge would
 * be cut by it.
 */
private val Carried = Easing { t -> t + (t * t * (3f - 2f * t) - t) * 0.85f }
private val StraightenLate = CubicBezierEasing(0.7f, 0f, 1f, 1f)
private val RoundEarly = CubicBezierEasing(0f, 0f, 0.3f, 1f)

private const val TIMELINE_MS = 400
private const val ABOVE_CONTAINER = 2f

/** How much the full screen grows per unit of overshoot, and how far down the bar dips. */
private const val EXPANDED_SWELL = 0.35f
private const val COLLAPSED_DIP = 0.12f

private val BarCorner = 28.dp
private val BarArtworkCorner = 16.dp
private val ScreenArtworkCorner = 28.dp
private val FlingThreshold = 125.dp

private enum class SharedKey {
    Container,
    Artwork,
    Title,
    PlayButton,
}
