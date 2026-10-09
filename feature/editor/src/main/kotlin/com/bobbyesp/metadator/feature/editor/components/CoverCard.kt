/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.ActionButtonGroup
import com.bobbyesp.metadator.core.designsystem.component.GroupAction
import com.bobbyesp.metadator.core.designsystem.component.LabelChip
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.feature.editor.ImageInfo
import com.bobbyesp.metadator.feature.editor.R

/**
 * The cover, what it is, and what can be done with it. Sits at the top of the editor.
 *
 * What can be done with it is in sight, as one group of buttons: changing it is the one with a
 * name, the rest are their icons. None of them is filled, since the screen's own main action is
 * saving, from its toolbar. Going back to the file's cover is on the cover itself, while it is not
 * the file's.
 */
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
    val motion = MaterialTheme.motionScheme
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
            androidx.compose.animation.AnimatedVisibility(
                visible = coverChanged && hasOriginalCover,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.medium),
                enter =
                    scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f) +
                        fadeIn(motion.fastEffectsSpec()),
                exit =
                    scaleOut(motion.fastEffectsSpec(), targetScale = 0.6f) +
                        fadeOut(motion.fastEffectsSpec()),
            ) {
                FilledTonalIconButton(
                    onClick = onRevertCover,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(R.string.cover_revert),
                    )
                }
            }
            FilledIconToggleButton(
                checked = isPlaying,
                onCheckedChange = { onPlay() },
                shapes = IconButtonDefaults.toggleableShapes(),
                // A toggle only for its shape: it stays in the primary color while playing too.
                colors =
                    IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
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

        ActionButtonGroup(
            primary =
                GroupAction(
                    stringResource(R.string.cover_change),
                    Icons.Rounded.Image,
                    onClick = onChangeCover,
                ),
            secondary =
                listOfNotNull(
                    GroupAction(
                        stringResource(R.string.cover_find_online),
                        Icons.Rounded.TravelExplore,
                        labeled = false,
                        onClick = onFindCover,
                    ),
                    GroupAction(
                            stringResource(R.string.cover_remove),
                            Icons.Rounded.Delete,
                            labeled = false,
                            onClick = onRemoveCover,
                        )
                        .takeIf { cover != null },
                ),
            modifier = Modifier.widthIn(max = ActionsMaxWidth).padding(top = Spacing.small),
            prominent = false,
        )
    }
}

/** What is known about the cover, as labels: its size, and whether it is the one in the file. */
@Composable
private fun CoverFacts(cover: ByteArray?, info: ImageInfo?, changed: Boolean, warnSmall: Boolean) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        if (cover == null) LabelChip(stringResource(R.string.cover_none), Icons.Rounded.HideImage)
        if (info != null) {
            LabelChip(
                stringResource(R.string.cover_size, info.width, info.height),
                Icons.Rounded.AspectRatio,
            )
        }
        if (changed) LabelChip(stringResource(R.string.changed), Icons.Rounded.Edit)
    }
    if (warnSmall && info?.isSmall == true) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp))
                Text(
                    stringResource(R.string.cover_small_warning),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** As wide as the group gets on a wide window, where it would otherwise span the whole pane. */
private val ActionsMaxWidth = 420.dp
