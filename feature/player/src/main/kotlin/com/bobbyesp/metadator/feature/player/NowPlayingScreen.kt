/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.common.formatDuration
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.component.sidesOf
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController
import com.bobbyesp.metadator.player.api.RepeatMode
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * The player, full screen. It is the expanded content of [PlayerSheet], which owns how it gets on
 * and off the screen; the modifiers are how the sheet ties the pieces it shares with the bar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NowPlayingContent(
    playback: PlaybackState,
    player: PlayerController,
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    topBarModifier: Modifier = Modifier,
    artworkShape: Shape = MaterialTheme.shapes.extraLarge,
    artworkModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
) {
    val track = playback.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = topBarModifier,
                title = { Text(stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.close))
                    }
                },
            )
        },
    ) { padding ->
        if (track == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(Spacing.extraLarge),
                verticalArrangement = Arrangement.Center,
            ) {
                PlaceholderCard(
                    title = stringResource(R.string.nothing_playing),
                    description = "",
                    icon = Icons.Rounded.PlayArrow,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().sidesOf(padding),
            contentPadding =
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    start = Spacing.extraLarge,
                    end = Spacing.extraLarge,
                    bottom = Spacing.huge,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "player") {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                ) {
                    Cover(track, playback.isPlaying, artworkShape, artworkModifier)
                    TrackTitle(track, titleModifier)
                    SeekBar(playback, onSeek = player::seekTo)
                    TransportControls(playback, player, playButtonModifier)
                    SecondaryControls(playback, player, onEdit = { onEdit(track.ref.uri) })
                }
            }
            if (playback.hasUpNext) {
                item(key = "up-next") {
                    Text(
                        stringResource(R.string.up_next),
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.widthIn(max = 480.dp)
                                .fillMaxWidth()
                                .padding(top = Spacing.extraLarge, bottom = Spacing.small),
                    )
                }
                upNextItems(playback, player, Modifier.widthIn(max = 480.dp))
            }
        }
    }
}

private const val MAX_UP_NEXT = 30

internal val PlaybackState.hasUpNext: Boolean
    get() = currentIndex < queue.lastIndex

/** What plays after the current song, as one group of rows; tapping one jumps to it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.upNextItems(
    playback: PlaybackState,
    player: PlayerController,
    itemModifier: Modifier = Modifier,
) {
    val upNext = playback.queue.drop(playback.currentIndex + 1).take(MAX_UP_NEXT)
    itemsIndexed(upNext, key = { index, it -> "${it.id.value}:$index" }) { index, next ->
        SegmentedListItem(
            onClick = { player.skipToQueueItem(playback.currentIndex + 1 + index) },
            shapes = GroupShapes.listItemShapes(index, upNext.size),
            modifier = itemModifier,
            verticalAlignment = Alignment.CenterVertically,
            colors =
                ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            leadingContent = { ArtworkImage(next.artworkRef?.uri, null, Modifier.size(44.dp)) },
            supportingContent = {
                Text(next.artist ?: stringResource(R.string.unknown_artist), maxLines = 1)
            },
        ) {
            Text(next.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The cover steps back while the music is paused and comes forward again when it plays.
 *
 * @param sharedModifier what ties it to the bar's cover, applied once it has its size
 * @param modifier how it takes its size, which is what differs between layouts
 */
@Composable
internal fun Cover(
    track: Track,
    isPlaying: Boolean,
    shape: Shape,
    sharedModifier: Modifier,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1f),
) {
    val scale by
        animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.86f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            label = "CoverScale",
        )
    ArtworkImage(
        model = track.artworkRef?.uri,
        contentDescription = null,
        modifier =
            modifier.then(sharedModifier).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        cacheKey = track.artworkCacheKey,
    )
}

@Composable
internal fun TrackTitle(track: Track, modifier: Modifier) {
    val motion = MaterialTheme.motionScheme
    // Centered by this box rather than by the text's alignment: the bounds the name travels to
    // are then the text's own, and the same text at another size scales into them exactly.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                (slideInVertically(motion.defaultSpatialSpec()) { it / 2 } +
                    fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutVertically(motion.fastSpatialSpec()) { -it / 2 } +
                        fadeOut(motion.fastEffectsSpec()))
            },
            contentKey = { it.id },
            modifier = modifier,
            label = "TrackTitle",
        ) { shown ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    shown.title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(shown.artist, shown.album).joinToString(" · ").ifEmpty {
                        stringResource(R.string.unknown_artist)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The position in the song, as a wave that moves while it plays and lies flat while it is paused or
 * being dragged.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SeekBar(
    playback: PlaybackState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showTimes: Boolean = true,
) {
    val state = rememberSliderState()
    // Where the user let go, until the player reports it: without this the thumb jumps back to the
    // old position for the moment the seek takes.
    var sought by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(playback.progress, state.isDragging, sought) {
        val target = sought
        if (target != null && abs(playback.progress - target) < SEEK_TOLERANCE) sought = null
        if (!state.isDragging && sought == null) state.value = playback.progress
    }
    LaunchedEffect(sought) {
        if (sought != null) {
            delay(SEEK_TIMEOUT_MS)
            sought = null
        }
    }

    val waving = playback.isPlaying && !state.isDragging
    Column(modifier) {
        Slider(
            state = state,
            onValueChange = { state.value = it },
            onValueChangeFinished = {
                sought = state.value
                onSeek((state.value * playback.durationMs).toLong())
            },
            enabled = playback.durationMs > 0,
            track = { slider ->
                LinearWavyProgressIndicator(
                    progress = { slider.coercedValueAsFraction },
                    modifier = Modifier.fillMaxWidth(),
                    amplitude = { if (waving) 1f else 0f },
                )
            },
        )
        if (showTimes)
            Row(Modifier.fillMaxWidth()) {
                Text(
                    formatDuration((state.value * playback.durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatDuration(playback.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

private const val SEEK_TOLERANCE = 0.02f
private const val SEEK_TIMEOUT_MS = 1_500L

/**
 * Previous, play and next as one button group: pressing one squeezes its neighbours, which is the
 * whole point of putting them in a group.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TransportControls(
    playback: PlaybackState,
    player: PlayerController,
    playButtonModifier: Modifier,
) {
    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        customItem(
            buttonGroupContent = {
                val interactions = remember { MutableInteractionSource() }
                FilledTonalIconButton(
                    onClick = player::skipToPrevious,
                    shapes = IconButtonDefaults.shapes(),
                    interactionSource = interactions,
                    modifier =
                        Modifier.weight(1f).height(TransportHeight).animateWidth(interactions),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        stringResource(R.string.previous),
                        Modifier.size(TransportIconSize),
                    )
                }
            },
            menuContent = {},
        )
        customItem(
            buttonGroupContent = {
                val interactions = remember { MutableInteractionSource() }
                PlayPauseButton(
                    isPlaying = playback.isPlaying,
                    onToggle = player::togglePlayPause,
                    iconSize = 40.dp,
                    interactionSource = interactions,
                    modifier =
                        Modifier.weight(1.5f)
                            .height(TransportHeight)
                            .animateWidth(interactions)
                            .then(playButtonModifier),
                )
            },
            menuContent = {},
        )
        customItem(
            buttonGroupContent = {
                val interactions = remember { MutableInteractionSource() }
                FilledTonalIconButton(
                    onClick = player::skipToNext,
                    enabled = playback.hasNext,
                    shapes = IconButtonDefaults.shapes(),
                    interactionSource = interactions,
                    modifier =
                        Modifier.weight(1f).height(TransportHeight).animateWidth(interactions),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        stringResource(R.string.next),
                        Modifier.size(TransportIconSize),
                    )
                }
            },
            menuContent = {},
        )
    }
}

private val TransportHeight = 80.dp
private val TransportIconSize = 32.dp

/** What changes how the queue plays, and the way out to the editor. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SecondaryControls(
    playback: PlaybackState,
    player: PlayerController,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShuffleButton(playback, player)
        FilledTonalButton(onClick = onEdit, shapes = ButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(ButtonDefaults.IconSize))
            Text(
                stringResource(R.string.edit_tags),
                Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
        RepeatButton(playback, player)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShuffleButton(playback: PlaybackState, player: PlayerController) {
    FilledTonalIconToggleButton(
        checked = playback.shuffle,
        onCheckedChange = player::setShuffle,
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        Icon(Icons.Rounded.Shuffle, stringResource(R.string.shuffle))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RepeatButton(playback: PlaybackState, player: PlayerController) {
    FilledTonalIconToggleButton(
        checked = playback.repeatMode != RepeatMode.Off,
        onCheckedChange = { player.setRepeatMode(playback.repeatMode.next()) },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        Icon(
            if (playback.repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne
            else Icons.Rounded.Repeat,
            stringResource(
                when (playback.repeatMode) {
                    RepeatMode.Off -> R.string.repeat_off
                    RepeatMode.All -> R.string.repeat_all
                    RepeatMode.One -> R.string.repeat_one
                }
            ),
        )
    }
}
