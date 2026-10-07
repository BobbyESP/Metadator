package com.bobbyesp.metadator.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Equalizer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.R

/**
 * A song in a list. Tapping edits it, the play button plays it, a long press starts selecting.
 * A song missing essential tags says so under its name, so the library shows what to fix next.
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
) {
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
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                ArtworkImage(
                    model = track.artworkRef?.uri,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.medium,
                )
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
        supportingContent = { TrackSubtitle(track) },
        trailingContent = {
            AnimatedContent(targetState = selecting, label = "TrackTrailing") { isSelecting ->
                if (!isSelecting) {
                    IconButton(onClick = onPlay, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Equalizer else Icons.Rounded.PlayArrow,
                            contentDescription =
                                if (isPlaying) stringResource(R.string.now_playing)
                                else stringResource(R.string.play),
                            tint =
                                if (isPlaying) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TrackSubtitle(track: Track) {
    val subtitle =
        listOf(
                track.artist ?: stringResource(R.string.unknown_artist),
                track.album ?: stringResource(R.string.unknown_album),
            )
            .joinToString(" · ")
    if (!track.needsAttention) {
        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val missing = stringResource(R.string.needs_attention)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "$subtitle. $missing" },
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp),
        )
        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
