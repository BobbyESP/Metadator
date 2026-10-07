/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.common.formatDuration
import com.bobbyesp.metadator.core.common.formatFileSize
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.editor.LoadedTrack
import com.bobbyesp.metadator.feature.editor.R
import java.util.Locale

/** One thing known about the audio: what it measures, and how much of it there is. */
private class FileStat(val icon: ImageVector, val label: String, val value: String)

/**
 * What the file is, read-only: its name under a badge of its format, what is known about its audio
 * as a grid of figures, and where it is.
 *
 * A group like the app's lists, rounded outside and tight inside, rather than one card of label and
 * value rows: the name and the folder are text to read (and to copy, so both can be selected), the
 * rest are figures to glance at, and a table gave all of them the same weight.
 */
@Composable
fun FileInfoCard(loaded: LoadedTrack, modifier: Modifier = Modifier) {
    val name = loaded.track?.displayName ?: loaded.file?.displayName.orEmpty()
    val stats = fileStats(loaded)
    val location = loaded.track?.folder ?: loaded.file?.location
    // A figure with no neighbour takes the whole row.
    val statRows = stats.chunked(StatsPerRow)
    val pieces = 1 + statRows.size + if (location != null) 1 else 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        FilePiece(shape = pieceShape(first = true, last = pieces == 1)) {
            Row(
                modifier = Modifier.padding(Spacing.large),
                horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormatBadge(loaded.fileExtension)
                SelectionContainer(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        statRows.forEachIndexed { rowIndex, row ->
            val last = rowIndex + 1 == pieces - 1
            Row(
                // Every figure of a row as tall as the tallest, whose label may wrap.
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                row.forEachIndexed { index, stat ->
                    StatTile(
                        stat = stat,
                        shape =
                            pieceShape(
                                last = last,
                                start = index == 0,
                                end = index == row.lastIndex,
                            ),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }

        if (location != null) {
            FilePiece(shape = pieceShape(last = true)) {
                Row(
                    modifier = Modifier.padding(Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.file_location),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SelectionContainer {
                            Text(location, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** What is known about the audio, in the order it is asked about; what is not known is left out. */
@Composable
private fun fileStats(loaded: LoadedTrack): List<FileStat> {
    val audio = loaded.audio
    return buildList {
        (audio?.durationMs ?: loaded.track?.durationMs)
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    FileStat(
                        Icons.Rounded.Schedule,
                        stringResource(R.string.file_duration),
                        formatDuration(it),
                    )
                )
            }
        (loaded.track?.sizeBytes ?: loaded.file?.sizeBytes)
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    FileStat(
                        Icons.Rounded.SdStorage,
                        stringResource(R.string.file_size),
                        formatFileSize(it),
                    )
                )
            }
        audio
            ?.bitrateKbps
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    FileStat(
                        Icons.Rounded.Speed,
                        stringResource(R.string.file_bitrate),
                        stringResource(R.string.file_bitrate_value, it),
                    )
                )
            }
        audio
            ?.sampleRateHz
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    FileStat(
                        Icons.Rounded.GraphicEq,
                        stringResource(R.string.file_sample_rate),
                        stringResource(
                            R.string.file_sample_rate_value,
                            (it / 1000.0).toString().removeSuffix(".0"),
                        ),
                    )
                )
            }
        audio
            ?.channels
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    FileStat(
                        Icons.Rounded.Speaker,
                        stringResource(R.string.file_channels),
                        when (it) {
                            1 -> stringResource(R.string.file_channels_mono)
                            2 -> stringResource(R.string.file_channels_stereo)
                            else -> it.toString()
                        },
                    )
                )
            }
    }
}

/** The format as the file's badge: its extension, or a file where it has none. */
@Composable
private fun FormatBadge(extension: String?) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Box(
            modifier =
                Modifier.defaultMinSize(minWidth = BadgeSize, minHeight = BadgeSize)
                    .padding(horizontal = Spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            if (extension != null) {
                Text(
                    extension.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            } else {
                Icon(Icons.Rounded.AudioFile, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StatTile(stat: FileStat, shape: Shape, modifier: Modifier = Modifier) {
    FilePiece(
        shape = shape,
        // Read as one thing: "Bitrate, 320 kbps".
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Icon(
                stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.extraSmall).size(20.dp),
            )
            Text(
                stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stat.value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun FilePiece(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

/**
 * The shape of a piece by where it is in the group: a corner is round where it is one of the
 * group's own, and tight where it meets another piece. As `GroupShapes` does for a list, with the
 * columns a list does not have.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun pieceShape(
    first: Boolean = false,
    last: Boolean = false,
    start: Boolean = true,
    end: Boolean = true,
): Shape {
    val outer = MaterialTheme.shapes.largeIncreased.topStart
    val inner = MaterialTheme.shapes.extraSmall.topStart
    fun corner(isOuter: Boolean): CornerSize = if (isOuter) outer else inner
    return RoundedCornerShape(
        topStart = corner(first && start),
        topEnd = corner(first && end),
        bottomEnd = corner(last && end),
        bottomStart = corner(last && start),
    )
}

private const val StatsPerRow = 2
private val BadgeSize = 48.dp
