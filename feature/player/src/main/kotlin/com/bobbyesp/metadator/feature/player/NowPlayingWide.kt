/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.Choice
import com.bobbyesp.metadator.core.designsystem.component.ConnectedChoices
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorBlurDefaults
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.dissolved
import com.bobbyesp.metadator.core.designsystem.theme.frosted
import com.bobbyesp.metadator.core.domain.lyrics.TrackLyrics
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * The player, full screen, where the window is wider than it is tall: the song on the left, and on
 * the right its lyrics or what plays next. Like [NowPlayingContent] it is the expanded content of
 * [PlayerSheet], and each of the modifiers the sheet ties the bar's pieces with goes on exactly one
 * element here too.
 *
 * @param compact whether the controls do not fit under the cover and float over the lyrics instead
 * @param settled whether the sheet is at rest, open. Only then is anything frosted: while the sheet
 *   moves its content is drawn in the transition's overlay, where a frosted surface flickers. And
 *   only then does the album show.
 * @param ownContentPresence how much of what only this has is there, as in [NowPlayingContent]
 */
@Composable
internal fun NowPlayingWideContent(
    track: Track,
    playback: PlaybackState,
    positionMs: () -> Long,
    player: PlayerController,
    lyrics: TrackLyrics?,
    compact: Boolean,
    settled: Boolean,
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    topBarModifier: Modifier = Modifier,
    artworkShape: Shape = MaterialTheme.shapes.extraLarge,
    artworkModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    ownContentPresence: () -> Float = { 1f },
) {
    // What the floating controls frost: the backdrop and the lyrics. Both are recorded here as
    // siblings of the controls, never as their ancestors, or the controls would blur themselves.
    val haze = rememberHazeState()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            PlayerBackdrop(track, Modifier.matchParentSize().hazeSource(haze))
            Row(
                modifier =
                    Modifier.fillMaxSize()
                        // Over the backdrop, which is the surface and stays as it is.
                        .dissolved(ownContentPresence)
                        .safeDrawingPadding()
                        .padding(horizontal = Spacing.extraLarge),
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
            ) {
                SongColumn(
                    track = track,
                    playback = playback,
                    positionMs = positionMs,
                    player = player,
                    withControls = !compact,
                    settled = settled,
                    onClose = onClose,
                    onEdit = { onEdit(track.ref.uri) },
                    modifier = Modifier.weight(SONG_COLUMN_WEIGHT).fillMaxHeight(),
                    topBarModifier = topBarModifier,
                    artworkShape = artworkShape,
                    artworkModifier = artworkModifier,
                    titleModifier = titleModifier,
                    artistModifier = artistModifier,
                    playButtonModifier = playButtonModifier,
                )
                SidePanel(
                    playback = playback,
                    positionMs = positionMs,
                    player = player,
                    lyrics = lyrics,
                    compact = compact,
                    settled = settled,
                    haze = haze,
                    onEdit = { onEdit(track.ref.uri) },
                    playButtonModifier = playButtonModifier,
                    modifier = Modifier.weight(1f - SONG_COLUMN_WEIGHT).fillMaxHeight(),
                )
            }
        }
    }
}

/** The song: its cover and its name, and under them its controls where they fit. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SongColumn(
    track: Track,
    playback: PlaybackState,
    positionMs: () -> Long,
    player: PlayerController,
    withControls: Boolean,
    settled: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
    topBarModifier: Modifier,
    artworkShape: Shape,
    artworkModifier: Modifier,
    titleModifier: Modifier,
    artistModifier: Modifier,
    playButtonModifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // The whole row is the handle the sheet is dragged down by, as the app bar is when stacked.
        Row(Modifier.fillMaxWidth().then(topBarModifier).padding(vertical = Spacing.small)) {
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.close))
            }
        }
        Column(
            modifier =
                Modifier.weight(1f)
                    .widthIn(max = SongColumnMaxWidth)
                    .fillMaxWidth()
                    .padding(bottom = Spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.large, Alignment.CenterVertically),
        ) {
            // The cover takes what height the rest leaves, and is as wide as it is tall.
            Cover(
                track = track,
                shape = artworkShape,
                sharedModifier = artworkModifier,
                modifier = Modifier.weight(1f, fill = false).aspectRatio(1f),
            )
            TrackName(track, settled, titleModifier, artistModifier)
            if (withControls) {
                SeekBar(playback, positionMs, onSeek = player::seekTo)
                TransportControls(playback, player, playButtonModifier)
                SecondaryControls(playback, player, onEdit)
            }
        }
    }
}

private enum class SidePane {
    Lyrics,
    UpNext,
}

/** The lyrics or the queue, whichever is chosen, and over them the controls when [compact]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SidePanel(
    playback: PlaybackState,
    positionMs: () -> Long,
    player: PlayerController,
    lyrics: TrackLyrics?,
    compact: Boolean,
    settled: Boolean,
    haze: HazeState,
    onEdit: () -> Unit,
    playButtonModifier: Modifier,
    modifier: Modifier,
) {
    var pane by rememberSaveable { mutableStateOf(SidePane.Lyrics) }
    val motion = MaterialTheme.motionScheme
    val lyricsLabel = stringResource(R.string.lyrics)
    val upNextLabel = stringResource(R.string.up_next)
    val panes =
        remember(lyricsLabel, upNextLabel) {
            listOf(
                Choice(SidePane.Lyrics, lyricsLabel, Icons.Rounded.Lyrics),
                Choice(SidePane.UpNext, upNextLabel, Icons.AutoMirrored.Rounded.QueueMusic),
            )
        }
    // What scrolls passes beneath the floating controls, padded to clear them at its end.
    val bottomClearance = if (compact) FloatingControlsClearance else 0.dp
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (compact) {
                    FilledTonalIconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.edit_tags))
                    }
                }
                ConnectedChoices(
                    choices = panes,
                    selected = pane,
                    onSelect = { pane = it },
                )
            }
            AnimatedContent(
                targetState = pane,
                transitionSpec = {
                    fadeIn(motion.defaultEffectsSpec()) togetherWith
                        fadeOut(motion.fastEffectsSpec())
                },
                modifier = Modifier.weight(1f).fillMaxWidth().hazeSource(haze, zIndex = 1f),
                label = "SidePane",
            ) { shown ->
                when (shown) {
                    SidePane.Lyrics ->
                        LyricsPane(
                            lyrics = lyrics,
                            playback = playback,
                            positionMs = positionMs,
                            onSeek = player::seekTo,
                            onEdit = onEdit,
                            compact = compact,
                            bottomClearance = bottomClearance,
                            modifier = Modifier.fillMaxSize(),
                        )
                    SidePane.UpNext -> UpNextPane(playback, player, bottomClearance)
                }
            }
        }
        if (compact) {
            FloatingControls(
                playback = playback,
                positionMs = positionMs,
                player = player,
                haze = haze,
                frost = settled,
                playButtonModifier = playButtonModifier,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.medium),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpNextPane(playback: PlaybackState, player: PlayerController, bottomClearance: Dp) {
    if (!playback.hasUpNext) {
        Box(Modifier.fillMaxSize().padding(bottom = bottomClearance), Alignment.Center) {
            Text(
                stringResource(R.string.queue_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(top = Spacing.small, bottom = Spacing.extraLarge + bottomClearance),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        upNextItems(playback, player)
    }
}

/**
 * The controls as one bar floating over the lyrics, frosted: what is beneath shows through it,
 * blurred. For where there is no height to put them under the cover.
 *
 * @param frost whether to frost it now; otherwise it is its container color, solid
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingControls(
    playback: PlaybackState,
    positionMs: () -> Long,
    player: PlayerController,
    haze: HazeState,
    frost: Boolean,
    playButtonModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = MaterialTheme.shapes.extraLarge
    val style = MetadatorBlurDefaults.surfaceStyle(container)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (frost) Modifier.frosted(haze, style, shape)
                    else Modifier.clip(shape).background(container)
                )
                .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = player::skipToPrevious, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous))
        }
        PlayPauseButton(
            isPlaying = playback.isPlaying,
            onToggle = player::togglePlayPause,
            // The sheet's modifier first: it is what sizes the button on the way here.
            modifier = playButtonModifier.size(PlayPauseButtonDefaults.CompactSize),
        )
        IconButton(
            onClick = player::skipToNext,
            enabled = playback.hasNext,
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.next))
        }
        SeekBar(
            playback = playback,
            positionMs = positionMs,
            onSeek = player::seekTo,
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.small),
            showTimes = false,
        )
        ShuffleButton(playback, player)
        RepeatButton(playback, player)
    }
}

/**
 * What the wide player is drawn on: the cover, out of focus, under a veil of the surface color, so
 * the screen takes the colors of the song without the text on it losing contrast. A new song's
 * cover fades in over the last one's. Where the device cannot blur, the theme's colors do, which
 * the cover has already tinted.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerBackdrop(track: Track, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val artwork = track.artworkRef?.uri
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || artwork == null) {
        Box(
            modifier.background(
                Brush.verticalGradient(listOf(colors.primaryContainer, colors.surface))
            )
        )
        return
    }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.background(colors.surface).onSizeChanged { bounds = it }) {
        Crossfade(
            targetState = track,
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            modifier = Modifier.align(Alignment.Center),
            label = "Backdrop",
        ) { shown ->
            // Blurred small and then stretched over the screen: the same picture as blurring it
            // at full size, for a fraction of the work on every frame the lyrics move. Stretched
            // past the edges, where a blur fades out.
            ArtworkImage(
                model = shown.artworkRef?.uri,
                contentDescription = null,
                modifier =
                    Modifier.size(BackdropSample)
                        .graphicsLayer {
                            val scale =
                                maxOf(bounds.width, bounds.height) / size.width * BACKDROP_OVERSCAN
                            scaleX = scale
                            scaleY = scale
                        }
                        .blur { radius = BlurRadiusSpec.uniform(BackdropBlur) },
                shape = RectangleShape,
                cacheKey = shown.artworkCacheKey,
            )
        }
        Box(Modifier.matchParentSize().background(colors.surface.copy(alpha = BACKDROP_VEIL)))
    }
}

private const val SONG_COLUMN_WEIGHT = 0.42f
private val SongColumnMaxWidth = 440.dp

/** The room the floating controls take at the bottom of what scrolls beneath them. */
private val FloatingControlsClearance = 80.dp

private val BackdropSample = 96.dp
private val BackdropBlur = 16.dp
private const val BACKDROP_OVERSCAN = 1.5f
private const val BACKDROP_VEIL = 0.72f
