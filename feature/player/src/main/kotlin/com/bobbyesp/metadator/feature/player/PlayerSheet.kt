/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.bobbyesp.metadator.core.designsystem.blur.BlurDefaults
import com.bobbyesp.metadator.core.designsystem.blur.BlurSource
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorAccentTheme
import com.bobbyesp.metadator.core.domain.lyrics.TrackLyrics
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * The player, over everything else: a bar while browsing, the whole screen when opened. Both are
 * one surface. Tapping the bar opens it; so does dragging it up, and then the bar's edges follow
 * the finger out to the edges of the screen, its corners straightening on the way.
 *
 * It is a shared element transition between the two contents of an `AnimatedContent`:
 * - The bar's surface and the screen's are one `sharedBounds`: the container. Its bounds are what
 *   animates, and what is inside either is laid out once, as it will be at rest, and clipped by it.
 * - The cover is a `sharedElement`: the same picture in both, so only one of them is drawn.
 * - The song's title, its artist and the play button are tied too, each on an arc, and of each only
 *   the version the sheet is going to is ever drawn, from the first frame. The two versions are
 *   made to be the same thing in the same bounds (the same text at another scale, the same button
 *   laid out again), so that swap does not show: it reads as one thing that moves and grows. Never
 *   both at once, one fading into the other: each has a bounds animation of its own, and seeked as
 *   this transition is they are not in step, so the two show apart, as a double image.
 * - What only one of the two has is not shared, and goes through focus instead
 *   ([com.bobbyesp.metadator.core.designsystem.blur.dissolved]): the bar's is gone early on the way
 *   up, the screen's comes in once the surface is its color.
 *
 * All of it is a function of how far open the sheet is, the same going up as coming down, since a
 * finger can stop it anywhere and turn back. The transition is seekable, which is what lets a drag
 * hold it halfway; [PlayerSheetState] is what moves it, and [SheetPieces] what it ties.
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
@Composable
fun PlayerSheet(
    visible: Boolean,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    layout: PlayerLayout = PlayerLayout.Stacked,
    expansion: PlayerSheetExpansion? = null,
    backdrop: BlurSource? = null,
) {
    val player: PlayerController = koinInject()
    val playback = rememberPlayerPlayback(player)
    val track = playback.current
    RequestNotificationsOnFirstPlay(active = playback.isActive)

    val sheet = rememberPlayerSheetState()
    val available = visible && track != null
    SheetEffects(sheet = sheet, available = available, expansion = expansion)

    val viewModel: NowPlayingViewModel = koinViewModel()
    val nowPlaying by viewModel.state.collectAsStateWithLifecycle()
    // On every new song, and each time the player is opened: the song's tags may have been edited
    // since they were read, and the editor is one tap away from here.
    val expanded by remember(sheet) { derivedStateOf { sheet.isExpanded } }
    LaunchedEffect(track?.ref, expanded) {
        if (track != null && (expanded || nowPlaying.ref != track.ref)) {
            viewModel.onIntent(NowPlayingIntent.Show(track))
        }
    }

    val barHeight = remember { mutableIntStateOf(0) }

    MetadatorAccentTheme(accent = nowPlaying.accent?.let(::Color)) {
        Box(
            modifier
                .fillMaxSize()
                .then(if (playback.isPlaying) Modifier.keepScreenOn() else Modifier)
                .onSizeChanged { sheet.travel = it.height.toFloat() }
                .bounce(sheet)
        ) {
            BarHalo(
                sheet = sheet,
                available = available,
                backdrop = backdrop,
                barHeight = barHeight.intValue,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                SheetContent(
                    sheet = sheet,
                    playback = playback,
                    player = player,
                    available = available,
                    haloed = backdrop != null && BlurDefaults.isHaloAvailable,
                    lyrics = nowPlaying.lyrics.takeIf { nowPlaying.ref == track?.ref },
                    layout = layout,
                    onEdit = onEdit,
                    onBarHeight = { barHeight.intValue = it },
                )
            }
        }
    }
}

/**
 * The bounce. The transition stops dead at either end, so what the spring does beyond them is shown
 * on everything at once: the screen swells a little past full, the bar dips a little past its
 * place.
 */
private fun Modifier.bounce(sheet: PlayerSheetState): Modifier = graphicsLayer {
    val overshoot = sheet.overshoot
    if (overshoot > 0f) {
        val scale = 1f + overshoot * EXPANDED_SWELL
        scaleX = scale
        scaleY = scale
    } else if (overshoot < 0f) {
        translationY = -overshoot * size.height * COLLAPSED_DIP
    }
}

/** The two contents of the sheet, one over the other while the transition runs. */
@Composable
private fun SharedTransitionScope.SheetContent(
    sheet: PlayerSheetState,
    playback: PlayerPlayback,
    player: PlayerController,
    available: Boolean,
    haloed: Boolean,
    lyrics: TrackLyrics?,
    layout: PlayerLayout,
    onEdit: (String) -> Unit,
    onBarHeight: (Int) -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val track = playback.current

    val pausedCoverScale =
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

    val transition = rememberTransition(sheet.transition, label = "PlayerSheet")
    transition.AnimatedContent(
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished
        },
        contentAlignment = Alignment.BottomCenter,
    ) { value ->
        val pieces =
            rememberSheetPieces(this, value == PlayerSheetValue.Collapsed, sheet, pausedCoverScale)
        when (value) {
            PlayerSheetValue.Collapsed ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    AnimatedVisibility(
                        visible = available,
                        modifier = Modifier.barPlace(),
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
                                playback = playback.state.value,
                                progress = playback.progress,
                                player = player,
                                onOpen = sheet::open,
                                shape = pieces.shape,
                                artworkShape = pieces.artworkShape,
                                shadowElevation = if (haloed) 0.dp else BarShadow,
                                modifier = pieces.container.then(dragToMove),
                                // Laid out as a bar throughout, at the top of the surface as it
                                // grows. Its size at rest is the halo's.
                                contentModifier =
                                    pieces.barContent.onSizeChanged { onBarHeight(it.height) },
                                artworkModifier = pieces.artwork,
                                titleModifier = pieces.title,
                                artistModifier = pieces.artist,
                                playButtonModifier = pieces.playButton,
                                ownContentPresence = barPresence,
                            )
                        }
                    }
                }

            PlayerSheetValue.Expanded -> {
                val edit = { uri: String ->
                    sheet.collapse()
                    onEdit(uri)
                }
                val content = pieces.container.then(pieces.screenContent)
                if (track == null || layout == PlayerLayout.Stacked) {
                    NowPlayingContent(
                        playback = playback.state.value,
                        positionMs = playback.positionMs,
                        player = player,
                        settled = sheet.isSettledExpanded,
                        onClose = sheet::collapse,
                        onEdit = edit,
                        modifier = content,
                        topBarModifier = dragToMove,
                        artworkShape = pieces.artworkShape,
                        artworkModifier = pieces.artwork,
                        titleModifier = pieces.title,
                        artistModifier = pieces.artist,
                        playButtonModifier = pieces.playButton,
                        ownContentPresence = screenPresence,
                    )
                } else {
                    NowPlayingWideContent(
                        track = track,
                        playback = playback.state.value,
                        positionMs = playback.positionMs,
                        player = player,
                        lyrics = lyrics,
                        compact = layout == PlayerLayout.SideBySideCompact,
                        settled = sheet.isSettledExpanded,
                        onClose = sheet::collapse,
                        onEdit = edit,
                        modifier = content,
                        topBarModifier = dragToMove,
                        artworkShape = pieces.artworkShape,
                        artworkModifier = pieces.artwork,
                        titleModifier = pieces.title,
                        artistModifier = pieces.artist,
                        playButtonModifier = pieces.playButton,
                        ownContentPresence = screenPresence,
                    )
                }
            }
        }
    }
}

/**
 * What is playing, split by how often it changes: [state] is everything but the position, which the
 * player reports twice a second, so it changes only when the song, the queue or the mode does.
 * Where the position shows, it is read through [positionMs] and [progress], while drawing.
 */
@Stable
internal class PlayerPlayback(
    val state: State<PlaybackState>,
    val positionMs: () -> Long,
    val progress: () -> Float,
) {
    val current: Track?
        get() = state.value.current

    val isPlaying: Boolean
        get() = state.value.isPlaying

    val isActive: Boolean
        get() = state.value.isActive
}

@Composable
private fun rememberPlayerPlayback(player: PlayerController): PlayerPlayback {
    val reported = player.state.collectAsStateWithLifecycle()
    return remember(reported) {
        val withoutPosition = derivedStateOf { reported.value.copy(positionMs = 0) }
        PlayerPlayback(
            state = withoutPosition,
            positionMs = { reported.value.positionMs },
            progress = { reported.value.progress },
        )
    }
}

/** What keeps the sheet honest with what is around it, none of which draws anything. */
@Composable
private fun SheetEffects(
    sheet: PlayerSheetState,
    available: Boolean,
    expansion: PlayerSheetExpansion?,
) {
    LaunchedEffect(available) { if (!available) sheet.snapToCollapsed() }
    LaunchedEffect(sheet) { sheet.sync() }
    CollapseOnBack(sheet)
    DisposableEffect(sheet, expansion) {
        expansion?.source = { sheet.fraction }
        onDispose { expansion?.source = null }
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
