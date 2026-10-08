/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.common.formatDuration
import com.bobbyesp.metadator.core.designsystem.component.LabelChip
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.component.RolledIn
import com.bobbyesp.metadator.core.designsystem.component.ThroughFocus
import com.bobbyesp.metadator.core.designsystem.component.sidesOf
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.defocused
import com.bobbyesp.metadator.core.designsystem.theme.dissolved
import com.bobbyesp.metadator.core.designsystem.theme.resizedTo
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController
import com.bobbyesp.metadator.player.api.RepeatMode
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * The player, full screen. It is the expanded content of [PlayerSheet], which owns how it gets on
 * and off the screen; the modifiers are how the sheet ties the pieces it shares with the bar.
 *
 * @param settled whether the sheet is at rest, open: what waits for the pieces that travel to have
 *   arrived shows then
 * @param ownContentPresence how much of what only this has (all but the cover, the name and the
 *   play button) is there, from 0 to 1, read while drawing: it comes into focus once the sheet has
 *   opened some way. The surface itself is not part of it, and is always solid.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NowPlayingContent(
    playback: PlaybackState,
    player: PlayerController,
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
    val track = playback.current
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        NowPlayingScaffold(
            playback = playback,
            player = player,
            track = track,
            settled = settled,
            onClose = onClose,
            onEdit = onEdit,
            modifier = Modifier.dissolved(ownContentPresence),
            topBarModifier = topBarModifier,
            artworkShape = artworkShape,
            artworkModifier = artworkModifier,
            titleModifier = titleModifier,
            artistModifier = artistModifier,
            playButtonModifier = playButtonModifier,
        )
    }
}

/** What is on the full player's surface, which is drawn apart so that this can come and go. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowPlayingScaffold(
    playback: PlaybackState,
    player: PlayerController,
    track: Track?,
    settled: Boolean,
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier,
    topBarModifier: Modifier,
    artworkShape: Shape,
    artworkModifier: Modifier,
    titleModifier: Modifier,
    artistModifier: Modifier,
    playButtonModifier: Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
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
                    Cover(track, artworkShape, artworkModifier)
                    TrackName(track, settled, titleModifier, artistModifier)
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
 * The playing song's cover, in either player. A new song's comes into focus over the old one's,
 * which goes out of focus beneath it and stays solid until it is covered: the cover is never seen
 * through, and never empty.
 *
 * @param modifier how it takes its size, and what ties it to the other player's
 */
@Composable
internal fun TrackArtwork(track: Track, shape: Shape, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = track,
        // Nothing of the container's own: each picture's focus is its transition, and the one
        // leaving has to wait for it.
        transitionSpec = {
            EnterTransition.None togetherWith
                ExitTransition.KeepUntilTransitionsFinished using
                SizeTransform(clip = false)
        },
        contentKey = { it.id },
        // The shape's clip is here and not on each picture: it is what a blur must not get past.
        modifier = modifier.clip(shape),
        label = "TrackArtwork",
    ) { shown ->
        val focus =
            transition.animateFloat(
                transitionSpec = { ThroughFocus.replacing() },
                label = "Focus",
            ) {
                if (it == EnterExitState.Visible) 1f else 0f
            }
        val blur = remember(focus) { { 1f - focus.value } }
        val leaving = transition.targetState == EnterExitState.PostExit
        ArtworkImage(
            model = shown.artworkRef?.uri,
            contentDescription = null,
            modifier =
                Modifier.fillMaxSize().defocused(blur, bounded = true).graphicsLayer {
                    alpha = if (leaving) 1f else focus.value
                },
            shape = RectangleShape,
            cacheKey = shown.artworkCacheKey,
        )
    }
}

/**
 * The song's cover. That it steps back while the music is paused is the sheet's doing, in
 * [sharedModifier]: the bar's cover has to do the same.
 *
 * @param sharedModifier what ties it to the bar's cover, applied once it has its size
 * @param modifier how it takes its size, which is what differs between layouts
 */
@Composable
internal fun Cover(
    track: Track,
    shape: Shape,
    sharedModifier: Modifier,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1f),
) {
    TrackArtwork(track, shape, modifier.then(sharedModifier))
}

/**
 * The song's title and, under it, who it is by, each a line of its own that the sheet ties to the
 * same line of the bar.
 *
 * Each is one line that says what the bar's says, set in a style the bar's is a smaller copy of
 * ([TrackNameDefaults]): scaled to the same height the two are then the same glyphs in the same
 * places, and the line travels whole. A title that wrapped here would be twice the shape of the
 * bar's, so one too long for the line scrolls instead.
 *
 * The album is this player's alone, so it is not part of what travels: it is a chip under the name,
 * which rolls into its place the first time the sheet settles. From then on it is one more thing on
 * this player, and comes and goes with the rest of it as the sheet is dragged. Its place is kept
 * for it meanwhile, so nothing below moves when it turns up.
 *
 * All of it starts at the leading edge, as it does in the bar. The title carries the weight and the
 * artist is set apart from it by its style, lighter in weight and in the color role for what
 * matters less, not by being made see-through.
 *
 * @param settled whether the sheet is at rest, open, which is when the album first shows
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TrackName(
    track: Track,
    settled: Boolean,
    titleModifier: Modifier,
    artistModifier: Modifier,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        TrackLine(track, titleModifier) { shown ->
            Text(
                shown.title,
                style = TrackNameDefaults.titleStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        TrackLine(track, artistModifier, follows = true) { shown ->
            Text(
                shown.artist ?: stringResource(R.string.unknown_artist),
                style = TrackNameDefaults.artistStyle,
                color = TrackNameDefaults.artistColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Once it has turned up it stays: a finger that starts to pull the sheet down must not send
        // it away, any more than it does the controls.
        var albumArrived by remember { mutableStateOf(false) }
        LaunchedEffect(settled) { if (settled) albumArrived = true }
        val album = track.album
        if (album != null) {
            // The chip's place, kept while it is not there. Not a window it is clipped to: a pill
            // cut by a rectangle on its way in shows the rectangle.
            Box(
                modifier = Modifier.padding(top = Spacing.small).height(AssistChipDefaults.Height),
                contentAlignment = Alignment.CenterStart,
            ) {
                RolledIn(album.takeIf { albumArrived }) { shown ->
                    LabelChip(label = shown, icon = Icons.Rounded.Album)
                }
            }
        }
    }
}

/**
 * One line about the playing song. A new song's comes in from the side through focus, a short way
 * and sharpening as it stops, while the old one's leaves the other way: the same as the album's
 * chip turning up, on its side.
 *
 * Nothing clips it. Slid the whole of its width inside its own box, the text was cut by the box's
 * edges as it went; a short way and out of focus, it needs no box to come out of.
 *
 * @param follows whether this is the second line of a pair, which then starts a moment after the
 *   first so that the two do not move as one block
 */
@Composable
internal fun TrackLine(
    track: Track,
    modifier: Modifier = Modifier,
    follows: Boolean = false,
    line: @Composable (Track) -> Unit,
) {
    val delay = if (follows) FOLLOW_MS else 0
    val shift = with(LocalDensity.current) { Spacing.extraLarge.roundToPx() }
    AnimatedContent(
        targetState = track,
        transitionSpec = {
            slideInHorizontally(ThroughFocus.arriving(delay)) { shift } togetherWith
                slideOutHorizontally(ThroughFocus.leaving(delay)) { -shift } using
                SizeTransform(clip = false)
        },
        contentKey = { it.id },
        modifier = modifier,
        label = "TrackLine",
    ) { shown ->
        val focus =
            transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) ThroughFocus.arriving(delay)
                    else ThroughFocus.leaving(delay)
                },
                label = "Focus",
            ) {
                if (it == EnterExitState.Visible) 1f else 0f
            }
        val presence = remember(focus) { { focus.value } }
        Box(Modifier.dissolved(presence)) { line(shown) }
    }
}

/** How much later the second line of a pair starts than the first, going and coming. */
private const val FOLLOW_MS = 140

/**
 * How the song's name is set in the full player, and in the bar: the same styles at the size of the
 * bar's roles in the type scale.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object TrackNameDefaults {
    /** No lighter than semibold: the title is what the eye should land on in either player. */
    val titleStyle: TextStyle
        @Composable
        get() =
            MaterialTheme.typography.headlineSmallEmphasized.copy(fontWeight = FontWeight.SemiBold)

    val artistStyle: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge

    /** The role for text that matters less, on whichever surface: a color, not an opacity. */
    val artistColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val barTitleStyle: TextStyle
        @Composable get() = titleStyle.resizedTo(MaterialTheme.typography.titleSmall.fontSize)

    val barArtistStyle: TextStyle
        @Composable get() = artistStyle.resizedTo(MaterialTheme.typography.bodySmall.fontSize)
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
            delay(SEEK_TIMEOUT_MS.milliseconds)
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
 * Previous, play and next as one button group: pressing one squeezes its neighbors, which is the
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
