/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.ActionMenu
import com.bobbyesp.metadator.core.designsystem.component.MenuAction
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.feature.editor.ImageInfo
import com.bobbyesp.metadator.feature.editor.R

/** The cover, what it is, and what can be done with it. Sits at the top of the editor. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverCard(
    title: String,
    artist: String?,
    cover: ByteArray?,
    coverInfo: ImageInfo?,
    coverChanged: Boolean,
    hasOriginalCover: Boolean,
    loadingCover: Boolean,
    warnSmall: Boolean,
    isPlaying: Boolean,
    onChangeCover: () -> Unit,
    onFindCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onRevertCover: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Box(
            Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkImage(
                model = cover,
                contentDescription = stringResource(R.string.cover),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.extraLarge,
            )
            if (loadingCover) LoadingIndicator()
            FilledIconToggleButton(
                checked = isPlaying,
                onCheckedChange = { onPlay() },
                shapes = IconButtonDefaults.toggleableShapes(),
                colors =
                    IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.medium).size(56.dp),
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription =
                        stringResource(if (isPlaying) R.string.pause_song else R.string.play_song),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (artist != null) {
                Text(
                    artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        CoverFacts(cover, coverInfo, coverChanged, warnSmall)

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = onChangeCover, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Image, null, Modifier.size(ButtonDefaults.IconSize))
                Text(
                    stringResource(R.string.cover_change),
                    Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_options))
                }
                ActionMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    actions =
                        listOfNotNull(
                            MenuAction(
                                stringResource(R.string.cover_find_online),
                                Icons.Rounded.TravelExplore,
                                onClick = onFindCover,
                            ),
                            MenuAction(
                                    stringResource(R.string.cover_remove),
                                    Icons.Rounded.Delete,
                                    onClick = onRemoveCover,
                                )
                                .takeIf { cover != null },
                            MenuAction(
                                    stringResource(R.string.cover_revert),
                                    Icons.AutoMirrored.Rounded.Undo,
                                    onClick = onRevertCover,
                                )
                                .takeIf { coverChanged && hasOriginalCover },
                        ),
                )
            }
        }
    }
}

@Composable
private fun CoverFacts(cover: ByteArray?, info: ImageInfo?, changed: Boolean, warnSmall: Boolean) {
    val parts = buildList {
        if (cover == null) add(stringResource(R.string.cover_none))
        info?.let { add(stringResource(R.string.cover_size, it.width, it.height)) }
        if (changed) add(stringResource(R.string.changed))
    }
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.labelLarge,
        color =
            if (changed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (warnSmall && info?.isSmall == true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Warning,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(R.string.cover_small_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
