/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.history

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.LabelChip
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.save.CoverChange
import com.bobbyesp.metadator.core.domain.save.EditHistoryEntry
import com.bobbyesp.metadator.core.domain.save.FieldChange
import com.bobbyesp.metadator.core.model.ItemKey
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.core.ui.component.fieldLabel
import com.bobbyesp.metadator.feature.editor.HistoryState
import com.bobbyesp.metadator.feature.editor.R
import com.bobbyesp.metadator.feature.editor.components.StateMessage
import com.bobbyesp.metadator.tags.api.BackupId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Edit history": the song's saves, newest first and by day, each with what it changed. Going back
 * to before one of them is asked for once more, since it rewrites the file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorySheet(
    history: HistoryState?,
    hasUnsavedChanges: Boolean,
    onRestore: (BackupId) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    var confirming by rememberSaveable { mutableStateOf<Long?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        HistoryList(history = history, onRestore = { confirming = it.id.value })
    }

    val entry = history?.entries?.firstOrNull { it.id.value == confirming }
    if (entry != null) {
        RestoreDialog(
            savedAtMillis = entry.savedAtMillis,
            hasUnsavedChanges = hasUnsavedChanges,
            onConfirm = {
                confirming = null
                onRestore(entry.id)
            },
            onDismiss = { confirming = null },
        )
    }
}

/** Shown expanded or not at all: half a history says little. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberHistorySheetState(): SheetState =
    rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

/** What the list of one day is made of: a save, and under the open one, what it changed. */
private data class Segment(val entry: EditHistoryEntry, val isChanges: Boolean)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryList(history: HistoryState?, onRestore: (EditHistoryEntry) -> Unit) {
    val entries = history?.entries
    val restoring = history?.restoring
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }
    val days = remember(entries) { entries?.byDay(ZoneId.systemDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
        contentPadding =
            PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.huge),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "header", contentType = "header") { Header() }
        when {
            history?.failed == true ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.ErrorOutline,
                        stringResource(R.string.history_loading_failed),
                        itemMotion(),
                        isError = true,
                    )
                }
            days == null ->
                item(key = "loading", contentType = "loading") {
                    ContainedLoadingIndicator(
                        itemMotion()
                            .fillMaxWidth()
                            .padding(Spacing.huge)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            days.isEmpty() ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.History,
                        stringResource(R.string.history_empty),
                        itemMotion(),
                    )
                }
            else ->
                days.forEach { (day, dayEntries) ->
                    item(key = ItemKey.of("day", day.toString()), contentType = "day") {
                        SectionHeader(dayLabel(day, dayEntries.first().savedAtMillis), itemMotion())
                    }
                    val segments = dayEntries.flatMap { entry ->
                        if (entry.id.value == expanded) {
                            listOf(Segment(entry, false), Segment(entry, true))
                        } else listOf(Segment(entry, false))
                    }
                    segments.forEachIndexed { index, (entry, isChanges) ->
                        val id = entry.id.value.toString()
                        if (isChanges) {
                            item(key = ItemKey.of("changes", id), contentType = "changes") {
                                Changes(
                                    entry = entry,
                                    restoring = restoring == entry.id,
                                    enabled = restoring == null,
                                    shape = GroupShapes.itemShape(index, segments.size),
                                    onRestore = { onRestore(entry) },
                                    modifier = itemMotion(),
                                )
                            }
                        } else {
                            item(key = ItemKey.of("version", id), contentType = "version") {
                                val isExpanded = entry.id.value == expanded
                                Version(
                                    entry = entry,
                                    expanded = isExpanded,
                                    enabled = restoring == null,
                                    shapes = GroupShapes.listItemShapes(index, segments.size),
                                    onClick = {
                                        expanded = if (isExpanded) null else entry.id.value
                                    },
                                    modifier = itemMotion(),
                                )
                            }
                        }
                    }
                }
        }
    }
}

/** Items come, go and move with the theme's motion, as one surface. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.itemMotion(): Modifier {
    val motion = MaterialTheme.motionScheme
    return Modifier.animateItem(
        fadeInSpec = motion.defaultEffectsSpec(),
        placementSpec = motion.defaultSpatialSpec(),
        fadeOutSpec = motion.fastEffectsSpec(),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header() {
    Column(
        modifier = Modifier.padding(bottom = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
            stringResource(R.string.history_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One save: the cover the song had before it, when, and which fields it touched. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Version(
    entry: EditHistoryEntry,
    expanded: Boolean,
    enabled: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val time =
        remember(context, entry.savedAtMillis) {
            DateUtils.formatDateTime(context, entry.savedAtMillis, DateUtils.FORMAT_SHOW_TIME)
        }
    val state =
        stringResource(if (expanded) R.string.history_expanded else R.string.history_collapsed)
    val rotation by
        animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "VersionChevron",
        )
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.semantics { stateDescription = state },
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        colors =
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        leadingContent = {
            ArtworkImage(
                model = entry.cover?.data,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        },
        overlineContent = { Text(time) },
        trailingContent = {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        },
    ) {
        Text(
            entry.summary(),
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The fields a save changed, then how many more; short enough for one line of a list. */
@Composable
private fun EditHistoryEntry.summary(): String {
    val labels =
        fields.map { fieldLabel(it.key) } +
            listOfNotNull(coverChange?.let { stringResource(R.string.history_cover) })
    return when {
        labels.isEmpty() -> stringResource(R.string.history_no_changes)
        labels.size <= SUMMARY_LABELS + 1 -> labels.joinToString(", ")
        else ->
            stringResource(
                R.string.history_more,
                labels.take(SUMMARY_LABELS).joinToString(", "),
                labels.size - SUMMARY_LABELS,
            )
    }
}

private const val SUMMARY_LABELS = 2

/**
 * What the open save changed, field by field, and the way back to before it. Part of the save's
 * group, so it reads as the save opened up.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Changes(
    entry: EditHistoryEntry,
    restoring: Boolean,
    enabled: Boolean,
    shape: Shape,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            entry.coverChange?.let { change ->
                LabelChip(label = stringResource(change.label), icon = Icons.Rounded.Image)
            }
            entry.fields.forEach { FieldChangeRow(it) }
            if (entry.fields.isEmpty() && entry.coverChange == null) {
                Text(
                    stringResource(R.string.history_no_changes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RestoreButton(
                restoring = restoring,
                enabled = enabled,
                onClick = onRestore,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

private val CoverChange.label: Int
    get() =
        when (this) {
            CoverChange.Added -> R.string.history_cover_added
            CoverChange.Replaced -> R.string.history_cover_replaced
            CoverChange.Removed -> R.string.history_cover_removed
        }

/** A field before and after the save, read out as one sentence: "Title, Halo, changed to …". */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FieldChangeRow(change: FieldChange, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        Text(
            fieldLabel(change.key),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FieldValue(change.before, emphasized = false)
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.history_changed_to),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            FieldValue(change.after, emphasized = true)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FieldValue(values: List<String>, emphasized: Boolean) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    if (values.isEmpty()) {
        Text(
            stringResource(R.string.lookup_empty_value),
            style = typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = colors.onSurfaceVariant,
        )
    } else {
        Text(
            values.joinToString(", "),
            style = if (emphasized) typography.bodyMediumEmphasized else typography.bodyMedium,
            color = if (emphasized) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestoreButton(
    restoring: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val restoringLabel = stringResource(R.string.history_restoring)
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier,
    ) {
        if (restoring) {
            LoadingIndicator(
                Modifier.size(ButtonDefaults.IconSize).semantics {
                    contentDescription = restoringLabel
                }
            )
        } else {
            Icon(Icons.Rounded.Restore, null, Modifier.size(ButtonDefaults.IconSize))
        }
        Text(
            stringResource(R.string.history_restore),
            Modifier.padding(start = ButtonDefaults.IconSpacing),
        )
    }
}

@Composable
private fun RestoreDialog(
    savedAtMillis: Long,
    hasUnsavedChanges: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val savedAt =
        remember(context, savedAtMillis) {
            DateUtils.formatDateTime(
                context,
                savedAtMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
            )
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restore, null) },
        title = { Text(stringResource(R.string.history_restore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Text(stringResource(R.string.history_restore_description, savedAt))
                if (hasUnsavedChanges) {
                    Text(
                        stringResource(R.string.history_restore_unsaved),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.history_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun List<EditHistoryEntry>.byDay(
    zone: ZoneId
): List<Pair<LocalDate, List<EditHistoryEntry>>> = groupBy {
    Instant.ofEpochMilli(it.savedAtMillis).atZone(zone).toLocalDate()
}
    .toList()

@Composable
private fun dayLabel(day: LocalDate, anyMillisOfDay: Long): String {
    val today = LocalDate.now()
    return when (day) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> {
            val context = LocalContext.current
            remember(context, day) { context.formatDay(anyMillisOfDay, day.year == today.year) }
        }
    }
}

private fun Context.formatDay(millis: Long, thisYear: Boolean): String =
    DateUtils.formatDateTime(
        this,
        millis,
        DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_DATE or
            if (thisYear) DateUtils.FORMAT_NO_YEAR else DateUtils.FORMAT_SHOW_YEAR,
    )
