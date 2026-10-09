/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.batch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.LoadingScreen
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.component.SwitchItem
import com.bobbyesp.metadator.core.designsystem.component.ToggleChip
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.component.readableWidth
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.batch.FileNamePattern
import com.bobbyesp.metadator.core.domain.batch.TrackNumbering
import com.bobbyesp.metadator.core.domain.save.SaveOutcome
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.core.ui.component.fieldLabel
import com.bobbyesp.metadator.tags.api.ArtworkChange
import com.bobbyesp.metadator.tags.api.TagField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BatchScreen(state: BatchState, onIntent: (BatchIntent) -> Unit, onClose: () -> Unit) {
    val pickCover =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onIntent(BatchIntent.SetCoverFromUri(uri.toString()))
        }
    val fileCount = state.files.size
    val canApply = state.canApply

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pluralStringResource(R.plurals.batch_title, fileCount, fileCount)) },
                navigationIcon = {
                    IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.close))
                    }
                },
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.screen)) {
                Button(
                    onClick = { onIntent(BatchIntent.Apply) },
                    enabled = canApply,
                    shapes = ButtonDefaults.shapes(),
                    modifier =
                        Modifier.readableWidth()
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(
                        stringResource(R.string.batch_apply),
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingScreen(Modifier.padding(padding))
            return@Scaffold
        }
        val patternPreview =
            remember(state.usePattern, state.pattern, state.files) {
                if (state.usePattern) state.patternPreview else emptyList()
            }
        LazyColumn(
            modifier =
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "files", contentType = "files") { FilesSummary(state.files) }
            item(key = "fields-header", contentType = "header") {
                SectionHeader(stringResource(R.string.batch_fields), Modifier.readableWidth())
            }
            item(key = "fields-description", contentType = "description") {
                Text(
                    stringResource(R.string.batch_fields_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.readableWidth().padding(bottom = Spacing.small),
                )
            }
            items(
                items = BatchViewModel.EditableFields,
                key = { it.key },
                contentType = { "batch_field" },
            ) { field ->
                BatchField(
                    field = field,
                    edited = state.edits[field.key],
                    common = state.common[field.key],
                    separator = state.separator,
                    onIntent = onIntent,
                )
            }

            item(key = "cover-header", contentType = "header") {
                SectionHeader(stringResource(R.string.batch_cover), Modifier.readableWidth())
            }
            item(key = "cover", contentType = "cover") {
                CoverChoice(
                    cover = state.cover,
                    onPick = {
                        pickCover.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onRemove = { onIntent(BatchIntent.RemoveCovers) },
                    onKeep = { onIntent(BatchIntent.KeepCovers) },
                )
            }
            item(key = "tools-header", contentType = "header") {
                SectionHeader(stringResource(R.string.batch_tools), Modifier.readableWidth())
            }
            item(key = "tools", contentType = "tools") {
                Tools(
                    fileCount = fileCount,
                    numbering = state.numbering,
                    usePattern = state.usePattern,
                    pattern = state.pattern,
                    patternPreview = patternPreview,
                    onIntent = onIntent,
                )
            }
        }
    }

    state.progress?.let { progress -> ProgressDialog(progress, onIntent) }
}

@Composable
private fun FilesSummary(files: List<BatchFile>) {
    val names = files.take(4).joinToString(", ") { it.title }
    val more = files.size - 4
    Text(
        if (more > 0) "$names ${stringResource(R.string.batch_and_more, more)}" else names,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.readableWidth().padding(vertical = Spacing.small),
    )
}

@Composable
private fun BatchField(
    field: TagField,
    edited: List<String>?,
    common: CommonValue?,
    separator: String,
    onIntent: (BatchIntent) -> Unit,
) {
    val label = fieldLabel(field.key)
    val shown =
        edited?.joinToString(separator)
            ?: (common as? CommonValue.Same)?.values?.joinToString(separator).orEmpty()
    TonalTextField(
        value = shown,
        onValueChange = { text ->
            val values =
                if (field.multiValue)
                    text.split(separator.trim()).map { it.trim() }.filter { it.isNotEmpty() }
                else listOf(text)
            onIntent(BatchIntent.SetField(field.key, values))
        },
        label = label,
        placeholder =
            if (common == CommonValue.Mixed && edited == null)
                stringResource(R.string.batch_multiple_values)
            else null,
        emphasized = edited != null,
        trailingIcon =
            if (edited != null) {
                {
                    IconButton(
                        onClick = { onIntent(BatchIntent.ResetField(field.key)) },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Undo,
                            stringResource(R.string.batch_reset_field, label),
                        )
                    }
                }
            } else null,
        singleLine = field != TagField.Comment,
        modifier = Modifier.readableWidth().padding(bottom = Spacing.small),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverChoice(
    cover: ArtworkChange,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onKeep: () -> Unit,
) {
    Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        when (cover) {
            is ArtworkChange.Replace ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    ArtworkImage(cover.picture.data, null, Modifier.size(72.dp))
                    Text(stringResource(R.string.batch_cover_will_replace), Modifier.weight(1f))
                }

            ArtworkChange.Remove ->
                Text(
                    stringResource(R.string.batch_cover_will_remove),
                    color = MaterialTheme.colorScheme.error,
                )

            ArtworkChange.Unchanged -> Unit
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            OutlinedButton(onClick = onPick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.batch_cover_set))
            }
            OutlinedButton(onClick = onRemove, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.batch_cover_remove))
            }
            if (cover != ArtworkChange.Unchanged) {
                TextButton(onClick = onKeep, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.batch_cover_keep))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tools(
    fileCount: Int,
    numbering: TrackNumbering?,
    usePattern: Boolean,
    pattern: String,
    patternPreview: List<Pair<String, Map<String, List<String>>?>>,
    onIntent: (BatchIntent) -> Unit,
) {
    Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        SwitchItem(
            title = stringResource(R.string.batch_numbering),
            supportingText = stringResource(R.string.batch_numbering_description, fileCount),
            icon = Icons.Rounded.FormatListNumbered,
            checked = numbering != null,
            onCheckedChange = {
                onIntent(BatchIntent.SetNumbering(if (it) TrackNumbering() else null))
            },
        )
        if (numbering != null) {
            TonalTextField(
                value = numbering.startAt.toString(),
                onValueChange = { text ->
                    text
                        .toIntOrNull()
                        ?.takeIf { it in 0..999 }
                        ?.let { onIntent(BatchIntent.SetNumbering(numbering.copy(startAt = it))) }
                },
                label = stringResource(R.string.batch_numbering_start),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(160.dp),
            )
        }
        SwitchItem(
            title = stringResource(R.string.batch_pattern),
            supportingText = stringResource(R.string.batch_pattern_description),
            icon = Icons.Rounded.TextFields,
            checked = usePattern,
            onCheckedChange = { onIntent(BatchIntent.UsePattern(it)) },
        )
        if (usePattern) {
            val valid = remember(pattern) { FileNamePattern(pattern).isValid }
            TonalTextField(
                value = pattern,
                onValueChange = { onIntent(BatchIntent.SetPattern(it)) },
                label = stringResource(R.string.batch_pattern_field),
                isError = !valid,
                supportingText =
                    if (!valid) stringResource(R.string.batch_pattern_invalid) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                FileNamePattern.Suggestions.forEach { suggestion ->
                    ToggleChip(
                        selected = suggestion == pattern,
                        onClick = { onIntent(BatchIntent.SetPattern(suggestion)) },
                        label = suggestion,
                    )
                }
            }
            patternPreview.forEach { (name, parsed) ->
                val described =
                    parsed?.entries?.map { (key, values) ->
                        fieldLabel(key) to values.joinToString()
                    }
                Column(Modifier.padding(vertical = Spacing.extraSmall)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        described?.joinToString(" · ") { (label, value) -> "$label: $value" }
                            ?: stringResource(R.string.batch_pattern_no_match),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (parsed == null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressDialog(
    progress: com.bobbyesp.metadator.core.domain.batch.BatchProgress,
    onIntent: (BatchIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon =
            if (!progress.isRunning) {
                { Icon(Icons.Rounded.CheckCircle, null) }
            } else null,
        title = {
            Text(
                if (progress.isRunning)
                    stringResource(R.string.batch_running, progress.done + 1, progress.total)
                else stringResource(R.string.batch_done_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                if (progress.isRunning) {
                    LinearWavyProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        pluralStringResource(
                            R.plurals.batch_saved,
                            progress.saved.size,
                            progress.saved.size,
                        )
                    )
                    if (progress.wasCancelled) Text(stringResource(R.string.batch_cancelled))
                    if (progress.failed.isNotEmpty()) {
                        Text(
                            pluralStringResource(
                                R.plurals.batch_failed,
                                progress.failed.size,
                                progress.failed.size,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                        progress.failed.take(5).forEach { result ->
                            Text(
                                "${result.item.displayName}: ${stringResource(reason(result.outcome))}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (progress.isRunning) {
                TextButton(onClick = { onIntent(BatchIntent.Cancel) }) {
                    Text(stringResource(R.string.batch_cancel))
                }
            } else {
                TextButton(onClick = { onIntent(BatchIntent.Dismiss) }) {
                    Text(stringResource(R.string.batch_finish))
                }
            }
        },
        dismissButton =
            if (!progress.isRunning && progress.backups.isNotEmpty()) {
                {
                    TextButton(onClick = { onIntent(BatchIntent.UndoAll) }) {
                        Text(stringResource(R.string.batch_undo_all))
                    }
                }
            } else null,
    )
}

private fun reason(outcome: SaveOutcome): Int =
    when (outcome) {
        SaveOutcome.NeedsAccess -> R.string.batch_reason_access
        SaveOutcome.FileGone -> R.string.batch_reason_gone
        SaveOutcome.Unsupported -> R.string.batch_reason_unsupported
        else -> R.string.batch_reason_failed
    }
