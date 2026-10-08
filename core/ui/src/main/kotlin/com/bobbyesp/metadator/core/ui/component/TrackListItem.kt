/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.PlayingBars
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.R

/**
 * A song in a list. Tapping edits it, the play button plays it, a long press starts selecting. A
 * song missing essential tags says which under its name, so the library shows what to fix next.
 *
 * @param isPlaybackRunning whether the playing song is actually sounding, not paused
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selecting: Boolean = false,
    isPlaying: Boolean = false,
    isPlaybackRunning: Boolean = isPlaying,
) {
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val selectedLabel = stringResource(R.string.selected)
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.semantics { if (selected) stateDescription = selectedLabel },
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.select),
        verticalAlignment = Alignment.CenterVertically,
        colors =
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        leadingContent = { TrackArtwork(track, selected) },
        supportingContent = { TrackSubtitle(track) },
        trailingContent = {
            AnimatedContent(
                targetState = selecting,
                transitionSpec = {
                    (scaleIn(spatial) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "TrackTrailing",
            ) { isSelecting ->
                if (!isSelecting) {
                    IconButton(onClick = onPlay, shapes = IconButtonDefaults.shapes()) {
                        if (isPlaying) {
                            val nowPlaying = stringResource(R.string.now_playing)
                            PlayingBars(
                                playing = isPlaybackRunning,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { contentDescription = nowPlaying },
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.play),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color =
                if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The cover, which becomes the row's checkbox while selecting: it rounds into a circle under a
 * check, so the thing the finger is on is the thing that answers.
 */
@Composable
private fun TrackArtwork(track: Track, selected: Boolean) {
    val motion = MaterialTheme.motionScheme
    val corner by
        animateDpAsState(
            targetValue = if (selected) ArtworkSize / 2 else 12.dp,
            animationSpec = motion.fastSpatialSpec(),
            label = "ArtworkCorner",
        )
    Box(contentAlignment = Alignment.Center) {
        ArtworkImage(
            model = track.artworkRef?.uri,
            contentDescription = null,
            modifier = Modifier.size(ArtworkSize),
            shape = RoundedCornerShape(corner),
        )
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastEffectsSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Box(
                Modifier.size(ArtworkSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

private val ArtworkSize = 52.dp

@Composable
private fun TrackSubtitle(track: Track) {
    val subtitle =
        listOf(
                track.artist ?: stringResource(R.string.unknown_artist),
                track.album ?: stringResource(R.string.unknown_album),
            )
            .joinToString(" · ")
    val missing = missingTagsText(track.missingFields)
    if (missing == null) {
        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    // Why it needs attention, under what is known of it: a warning alone left the reason to guess.
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
            Text(
                missing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A square cover with a title and a subtitle under it, for album grids. */
@Composable
fun CollectionCard(
    title: String,
    subtitle: String?,
    artwork: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            ArtworkImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.large,
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
