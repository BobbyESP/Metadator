/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ArcMode
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorAccentTheme
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorBlurDefaults
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.blurHalo
import com.bobbyesp.metadator.core.designsystem.theme.dissolved
import com.bobbyesp.metadator.player.api.PlayerController
import dev.chrisbanes.haze.HazeState
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
 * - The song's title, its artist and the play button are tied too, each on an arc, and of each only
 *   the version the sheet is going to is ever drawn, from the first frame. The two versions are
 *   made to be the same thing in the same bounds (the same text at another scale, the same button
 *   laid out again), so that swap does not show: it reads as one thing that moves and grows. Never
 *   both at once, one fading into the other: each has a bounds animation of its own, and seeked as
 *   this transition is they are not in step, so the two show apart, as a double image.
 * - What only one of the two has is not shared, and goes through focus instead ([dissolved]): the
 *   bar's is gone early on the way up, the screen's comes in once the surface is its color.
 *
 * All of it is a function of how far open the sheet is, the same going up as coming down, since a
 * finger can stop it anywhere and turn back.
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
 * @param backdrop what is recorded of the screens under the player, which the bar is lifted off
 *   with a blur halo; without it, or where the device cannot draw one, with a shadow
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerSheet(
    visible: Boolean,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    layout: PlayerLayout = PlayerLayout.Stacked,
    expansion: PlayerSheetExpansion? = null,
    backdrop: HazeState? = null,
) {
    val player: PlayerController = koinInject()
    val playbackState = player.state.collectAsStateWithLifecycle()
    // Without the position, which the player reports twice a second: where it shows, it is read
    // through positionMs and progress.
    val playback by
        remember(playbackState) { derivedStateOf { playbackState.value.copy(positionMs = 0) } }
    val positionMs = remember(playbackState) { { playbackState.value.positionMs } }
    val progress = remember(playbackState) { { playbackState.value.progress } }
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
    val expanded by remember(sheet) { derivedStateOf { sheet.isExpanded } }
    LaunchedEffect(track?.ref, expanded) {
        if (track != null) viewModel.onIntent(NowPlayingIntent.Show(track))
    }

    val motion = MaterialTheme.motionScheme

    val haloed = backdrop != null && MetadatorBlurDefaults.isHaloSupported
    // The cover steps back while the music is paused, in the full player only. Both covers do it
    // by as much as the sheet is open: a shared element draws one of the two from the first frame,
    // and if only the screen's stepped back, the cover would jump to its size there.
    val pausedCoverScale by
        animateFloatAsState(
            targetValue = if (playback.isPlaying) 1f else PAUSED_COVER_SCALE,
            animationSpec = motion.slowSpatialSpec(),
            label = "CoverScale",
        )
    val barPresence = remember(sheet) { { 1f - sheet.fraction / BAR_GONE_BY } }
    val screenPresence =
        remember(sheet) { { (sheet.fraction - SCREEN_FROM) / (SCREEN_BY - SCREEN_FROM) } }
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
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished
                },
                contentAlignment = Alignment.BottomCenter,
            ) { value ->
                val scope = this
                val isBar = value == PlayerSheetValue.Collapsed

                val arriving = scope.transition.targetState == EnterExitState.Visible

                val corner =
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
                val artworkCorner =
                    scope.transition.animateDp(
                        transitionSpec = { traveller() },
                        label = "Artwork",
                    ) {
                        if ((it == EnterExitState.Visible) == isBar) BarArtworkCorner
                        else ScreenArtworkCorner
                    }
                val shape = remember(corner) { CornerShape(corner) }
                val artworkShape = remember(artworkCorner) { CornerShape(artworkCorner) }
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
                val artwork =
                    Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(SharedKey.Artwork),
                            animatedVisibilityScope = scope,
                            boundsTransform = { _, _ -> traveller() },
                            zIndexInOverlay = ABOVE_CONTAINER,
                        )
                        .graphicsLayer {
                            val scale = lerp(1f, pausedCoverScale, sheet.fraction)
                            scaleX = scale
                            scaleY = scale
                        }
                val text =
                    SharedTransitionScope.ResizeMode.scaleToBounds(
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.CenterStart,
                    )
                val title = travellingText(SharedKey.Title, scope, text)
                val artist = travellingText(SharedKey.Artist, scope, text)
                val playButton =
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(SharedKey.PlayButton),
                        animatedVisibilityScope = scope,
                        boundsTransform = { _, _ -> arcTraveller() },
                        zIndexInOverlay = ABOVE_CONTAINER,
                    )

                when (value) {
                    PlayerSheetValue.Collapsed ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            BarPlace(
                                sheet = sheet,
                                available = available,
                                backdrop = backdrop,
                                modifier =
                                    Modifier.navigationBarsPadding()
                                        .padding(bottom = Spacing.medium)
                                        .widthIn(max = BarMaxWidth)
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.medium),
                            ) {
                                AnimatedVisibility(
                                    visible = available,
                                    // A spring, so the bar lands with a little bounce.
                                    enter =
                                        slideInVertically(motion.defaultSpatialSpec()) { it * 2 } +
                                            fadeIn(motion.defaultEffectsSpec()),
                                    exit =
                                        slideOutVertically(motion.fastSpatialSpec()) { it * 2 } +
                                            fadeOut(motion.fastEffectsSpec()),
                                ) {
                                    if (track != null) {
                                        MiniPlayerBar(
                                            track = track,
                                            playback = playback,
                                            progress = progress,
                                            player = player,
                                            onOpen = sheet::open,
                                            shape = shape,
                                            artworkShape = artworkShape,
                                            shadowElevation = if (haloed) 0.dp else BarShadow,
                                            modifier = container.then(dragToMove),
                                            // Laid out as a bar throughout, at the top of the
                                            // surface as it grows.
                                            contentModifier = Modifier.skipToLookaheadSize(),
                                            artworkModifier = artwork,
                                            titleModifier = title,
                                            artistModifier = artist,
                                            playButtonModifier = playButton,
                                            ownContentPresence = barPresence,
                                        )
                                    }
                                }
                            }
                        }
                    PlayerSheetValue.Expanded -> {
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
                                positionMs = positionMs,
                                player = player,
                                settled = sheet.isSettledExpanded,
                                onClose = sheet::collapse,
                                onEdit = edit,
                                modifier = content,
                                topBarModifier = dragToMove,
                                artworkShape = artworkShape,
                                artworkModifier = artwork,
                                titleModifier = title,
                                artistModifier = artist,
                                playButtonModifier = playButton,
                                ownContentPresence = screenPresence,
                            )
                        } else {
                            NowPlayingWideContent(
                                track = track,
                                playback = playback,
                                positionMs = positionMs,
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
                                artworkShape = artworkShape,
                                artworkModifier = artwork,
                                titleModifier = title,
                                artistModifier = artist,
                                playButtonModifier = playButton,
                                ownContentPresence = screenPresence,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Where the bar sits, lifted off what is behind it by a halo while it is at rest. The halo's
 * strength is animated here, so that only this follows it and not the bar.
 */
@Composable
private fun BarPlace(
    sheet: PlayerSheetState,
    available: Boolean,
    backdrop: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val atRest by remember(sheet) { derivedStateOf { sheet.fraction == 0f } }
    val strength by
        animateFloatAsState(
            targetValue = if (available && atRest) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "MiniPlayerHalo",
        )
    Box(
        modifier.then(
            if (backdrop != null && (available || strength > 0f)) {
                Modifier.blurHalo(state = backdrop, shape = BarShape, strength = strength)
            } else Modifier
        )
    ) {
        content()
    }
}

/** A rounded shape whose corner is read as it is drawn, so that animating it recomposes nothing. */
@Stable
private class CornerShape(private val corner: State<Dp>) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = RoundedCornerShape(corner.value).createOutline(size, layoutDirection, density)
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
            openSpec = OpenSpring,
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
    // Read as it is when it is delivered, not as a composition saw it: a quick gesture is over
    // before the frame that would have shown its last step.
    LaunchedEffect(backState, sheet) {
        snapshotFlow {
            (backState.transitionState as? NavigationEventTransitionState.InProgress)
                ?.latestEvent
                ?.progress
        }
            .collect { progress -> if (progress != null) sheet.previewCollapse(progress) }
    }
    val expanded by remember(sheet) { derivedStateOf { sheet.isExpanded } }
    NavigationBackHandler(
        state = backState,
        isBackEnabled = expanded,
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

/**
 * The pace of what the container carries, on a curve instead of a straight line: sideways first and
 * up late on the way to the screen, down first and sideways late on the way back, which is the same
 * path walked the other way.
 *
 * That way round and not the other because of the container: its top edge rises at a steady pace,
 * and what rose ahead of it would be cut by it. An arc that keeps low stays behind the edge.
 */
@OptIn(ExperimentalAnimationSpecApi::class)
private fun arcTraveller(): FiniteAnimationSpec<Rect> =
    ArcAnimationSpec(mode = ArcMode.ArcBelow, durationMillis = TIMELINE_MS, easing = Carried)

/**
 * Ties a line of text the bar and the full player both have. It is a shared element in all but
 * name: only the version the transition is going to is drawn, the other gone from the first frame.
 * `sharedElement` itself lays its content out again in the bounds as they change, which would wrap
 * or cut a text differently at every width; these bounds scale it.
 *
 * The two say the same, so the line travels whole, and it is not clipped to its bounds on the way:
 * they travel on an arc, which moves their corners apart and together again, so they do not keep
 * the shape of the text and would cut into it.
 *
 * @param resizeMode how the text is scaled into the bounds
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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

/**
 * How the sheet finishes once released. Looser than the motion scheme's springs, which are tuned
 * for a button or a card: this one moves the whole screen, starts with the finger's velocity, and
 * is meant to run a little past the end and come back.
 */
private val SettleSpring = spring<Float>(dampingRatio = 0.68f, stiffness = 240f)

/**
 * How the sheet opens from a tap on the bar. Softer than [SettleSpring], which finishes what a
 * finger had already started and at its speed: a tap starts from rest and covers the whole way.
 */
private val OpenSpring = spring<Float>(dampingRatio = 0.76f, stiffness = 110f)

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

/**
 * How far open the sheet is when what only the bar has is gone, and between which two points what
 * only the screen has comes into focus. The gap between them is the surface alone, changing color:
 * the two never show through each other.
 */
private const val BAR_GONE_BY = 0.2f
private const val SCREEN_FROM = 0.3f
private const val SCREEN_BY = 0.8f

/** The cover's size in the full player while the music is paused. */
private const val PAUSED_COVER_SCALE = 0.86f

/** How much the full screen grows per unit of overshoot, and how far down the bar dips. */
private const val EXPANDED_SWELL = 0.35f
private const val COLLAPSED_DIP = 0.12f

private val BarCorner = 28.dp
private val BarShape = RoundedCornerShape(BarCorner)
private val BarMaxWidth = 560.dp

/** The bar's shadow where no halo lifts it. */
private val BarShadow = 6.dp
private val BarArtworkCorner = 16.dp
private val ScreenArtworkCorner = 28.dp
private val FlingThreshold = 125.dp

private enum class SharedKey {
    Container,
    Artwork,
    Title,
    Artist,
    PlayButton,
}
