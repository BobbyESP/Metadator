/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.bobbyesp.metadator.core.designsystem.component.LoadingScreen
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.editor.Position
import com.bobbyesp.metadator.core.domain.editor.TagDraft
import com.bobbyesp.metadator.core.ui.R as CoreUiR
import com.bobbyesp.metadator.core.ui.component.fieldLabel
import com.bobbyesp.metadator.feature.editor.components.CoverCard
import com.bobbyesp.metadator.feature.editor.components.FileInfoCard
import com.bobbyesp.metadator.feature.editor.components.PositionField
import com.bobbyesp.metadator.feature.editor.components.TagChipsField
import com.bobbyesp.metadator.feature.editor.components.TagTextField
import com.bobbyesp.metadator.lyrics.api.Lrc
import com.bobbyesp.metadator.tags.api.FieldKind
import com.bobbyesp.metadator.tags.api.TagField

/** The fields of each section, in order. */
private val MainFields =
    listOf(
        TagField.Title,
        TagField.Artist,
        TagField.Album,
        TagField.AlbumArtist,
        TagField.Date,
        TagField.Genre,
    )
private val CreditFields =
    listOf(
        TagField.Composer,
        TagField.Lyricist,
        TagField.Conductor,
        TagField.Remixer,
        TagField.Performer,
    )
private val MoreFields =
    listOf(
        TagField.Comment,
        TagField.Bpm,
        TagField.Isrc,
        TagField.Copyright,
        TagField.Label,
        TagField.Grouping,
    )

/** Keys with a place of their own; everything else is under "All tags". */
private val KnownKeys: Set<String> =
    TagField.entries.map { it.key }.toSet() + TagField.TrackTotalKeys + TagField.DiscTotalKeys

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EditorScreen(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    onFindMetadata: () -> Unit,
    onClose: () -> Unit,
    showClose: Boolean,
    modifier: Modifier = Modifier,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val leave = {
        if (state.isDirty) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }

    // Back with unsaved changes asks first; without them it leaves as usual.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = state.isDirty,
        onBackCompleted = { confirmDiscard = true },
    )

    val pickCover =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onIntent(EditorIntent.SetCoverFromUri(uri.toString()))
        }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            EditorTopBar(
                title =
                    if (state.status == EditorStatus.Ready) state.title
                    else stringResource(R.string.edit_tags),
                showClose = showClose,
                canRevert = state.isDirty,
                onClose = leave,
                onRevertAll = { onIntent(EditorIntent.RevertAll) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val draft = state.draft
        val loaded = state.loaded
        when {
            state.status == EditorStatus.Loading -> LoadingScreen(Modifier.padding(padding))
            state.status != EditorStatus.Ready || draft == null || loaded == null ->
                EditorProblem(
                    state.status,
                    onRetry = { onIntent(EditorIntent.Reload) },
                    onClose = onClose,
                    modifier = Modifier.padding(padding),
                )
            else ->
                Box(
                    Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).imePadding()
                ) {
                    val cover =
                        @Composable { m: Modifier ->
                            CoverCard(
                                title = state.title,
                                artist = state.artist,
                                cover = draft.cover?.data,
                                coverInfo = state.coverInfo,
                                coverChanged = draft.isCoverChanged,
                                hasOriginalCover = draft.original.frontCover != null,
                                loadingCover = state.loadingCover,
                                warnSmall = state.settings.warnSmallArtwork,
                                isPlaying = state.isPlayingThis,
                                onChangeCover = {
                                    pickCover.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                onFindCover = onFindMetadata,
                                onRemoveCover = { onIntent(EditorIntent.RemoveCover) },
                                onRevertCover = { onIntent(EditorIntent.RevertCover) },
                                onPlay = { onIntent(EditorIntent.Play) },
                                modifier = m,
                            )
                        }
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        if (maxWidth >= TwoColumnWidth) {
                            // Wide: the cover and the file stay in view while the fields scroll.
                            Row(Modifier.fillMaxSize().padding(horizontal = Spacing.extraLarge)) {
                                Column(
                                    Modifier.width(320.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = Spacing.large),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
                                ) {
                                    cover(Modifier)
                                    FileInfoCard(loaded)
                                }
                                LazyColumn(
                                    modifier =
                                        Modifier.weight(1f).padding(start = Spacing.extraLarge),
                                    contentPadding =
                                        PaddingValues(bottom = Spacing.floatingClearance),
                                ) {
                                    fields(state, draft, onIntent)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding =
                                    PaddingValues(
                                        start = Spacing.screen,
                                        end = Spacing.screen,
                                        bottom = Spacing.floatingClearance,
                                    ),
                            ) {
                                item(key = "cover") {
                                    cover(Modifier.padding(vertical = Spacing.large))
                                }
                                fields(state, draft, onIntent)
                                item(key = "file-header") {
                                    SectionHeader(stringResource(R.string.section_file))
                                }
                                item(key = "file") { FileInfoCard(loaded) }
                            }
                        }
                    }

                    EditorToolbar(
                        dirty = state.isDirty,
                        saving = state.saving,
                        findingLyrics = state.findingLyrics,
                        onSave = { onIntent(EditorIntent.Save) },
                        onFindMetadata = onFindMetadata,
                        onFindLyrics = { onIntent(EditorIntent.FindLyrics) },
                        onRevertAll = { onIntent(EditorIntent.RevertAll) },
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = Spacing.large),
                    )
                }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            icon = { Icon(Icons.Rounded.Warning, null) },
            title = { Text(stringResource(R.string.unsaved_title)) },
            text = { Text(stringResource(R.string.unsaved_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onIntent(EditorIntent.RevertAll)
                        onClose()
                    }
                ) {
                    Text(stringResource(R.string.unsaved_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.unsaved_keep))
                }
            },
        )
    }
}

private val TwoColumnWidth = 760.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    showClose: Boolean,
    canRevert: Boolean,
    onClose: () -> Unit,
    onRevertAll: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    if (showClose) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.revert_all)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, null) },
                        enabled = canRevert,
                        onClick = {
                            menuOpen = false
                            onRevertAll()
                        },
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorToolbar(
    dirty: Boolean,
    saving: Boolean,
    findingLyrics: Boolean,
    onSave: () -> Unit,
    onFindMetadata: () -> Unit,
    onFindLyrics: () -> Unit,
    onRevertAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { if (dirty && !saving) onSave() }
            ) {
                if (saving) LoadingIndicator(color = MaterialTheme.colorScheme.onTertiaryContainer)
                else
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription =
                            stringResource(if (saving) R.string.saving else R.string.save),
                    )
            }
        },
    ) {
        IconButton(onClick = onFindMetadata, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.TravelExplore, stringResource(R.string.lookup_find))
        }
        IconButton(
            onClick = onFindLyrics,
            enabled = !findingLyrics,
            shapes = IconButtonDefaults.shapes(),
        ) {
            if (findingLyrics) LoadingIndicator()
            else Icon(Icons.Rounded.Lyrics, stringResource(R.string.lyrics_find))
        }
        IconButton(onClick = onRevertAll, enabled = dirty, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.revert_all))
        }
    }
}

/** Every editable section, as items of the editor's list. */
private fun LazyListScope.fields(
    state: EditorState,
    draft: TagDraft,
    onIntent: (EditorIntent) -> Unit,
) {
    val separator = state.settings.multiValueSeparator
    item(key = "main-header") { SectionHeader(stringResource(R.string.section_main)) }
    MainFields.forEach { field ->
        item(key = field.key) { FieldInput(field, draft, separator, onIntent) }
    }

    item(key = "track-header") { SectionHeader(stringResource(R.string.section_track)) }
    listOf(TagField.TrackNumber, TagField.DiscNumber).forEach { field ->
        item(key = field.key) {
            val position = state.position(field)
            val keys =
                listOf(field.key) +
                    if (field == TagField.TrackNumber) TagField.TrackTotalKeys
                    else TagField.DiscTotalKeys
            PositionField(
                label = fieldLabel(field.key),
                number = position.number,
                total = position.total,
                onChange = { number, total ->
                    onIntent(EditorIntent.SetPosition(field, Position(number, total)))
                },
                changed = keys.any(draft::isChanged),
                onRevert = { onIntent(EditorIntent.Revert(keys)) },
                modifier = Modifier.padding(bottom = Spacing.small),
            )
        }
    }

    item(key = "credits-header") { SectionHeader(stringResource(R.string.section_credits)) }
    CreditFields.forEach { field ->
        item(key = field.key) { FieldInput(field, draft, separator, onIntent) }
    }

    item(key = "more-header") { SectionHeader(stringResource(R.string.section_more)) }
    MoreFields.forEach { field ->
        item(key = field.key) { FieldInput(field, draft, separator, onIntent) }
    }

    item(key = "lyrics-header") {
        SectionHeader(
            stringResource(R.string.section_lyrics),
            trailing = {
                FilledTonalButton(
                    onClick = { onIntent(EditorIntent.FindLyrics) },
                    enabled = !state.findingLyrics,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.lyrics_find))
                }
            },
        )
    }
    item(key = TagField.Lyrics.key) {
        val lyrics = draft.tags.first(TagField.Lyrics.key).orEmpty()
        Column {
            if (Lrc.isSynced(lyrics)) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.lyrics_synced)) },
                    leadingIcon = { Icon(Icons.Rounded.Lyrics, null) },
                )
            }
            TagTextField(
                label = fieldLabel(TagField.Lyrics.key),
                value = lyrics,
                onValueChange = {
                    onIntent(EditorIntent.SetField(TagField.Lyrics.key, listOf(it)))
                },
                changed = draft.isChanged(TagField.Lyrics.key),
                onRevert = { onIntent(EditorIntent.Revert(listOf(TagField.Lyrics.key))) },
                singleLine = false,
                minLines = 4,
                maxLines = 14,
            )
        }
    }

    val otherKeys =
        (draft.tags.keys + draft.original.tags.keys).filter { it !in KnownKeys }.distinct().sorted()
    item(key = "all-header") {
        var adding by rememberSaveable { mutableStateOf(false) }
        SectionHeader(
            stringResource(R.string.section_all_tags),
            trailing = {
                IconButton(onClick = { adding = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add_field))
                }
            },
        )
        if (adding) {
            AddFieldDialog(
                onAdd = { key, value ->
                    adding = false
                    onIntent(EditorIntent.SetField(key, listOf(value)))
                },
                onDismiss = { adding = false },
            )
        }
    }
    item(key = "all-description") {
        Text(
            stringResource(R.string.all_tags_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.padding(
                    start = Spacing.small,
                    end = Spacing.small,
                    bottom = Spacing.small,
                ),
        )
    }
    otherKeys.forEach { key ->
        item(key = "raw:$key") {
            TagChipsField(
                label = key,
                values = draft.tags[key],
                onValuesChange = { onIntent(EditorIntent.SetField(key, it)) },
                changed = draft.isChanged(key),
                onRevert = { onIntent(EditorIntent.Revert(listOf(key))) },
                separator = separator,
                modifier = Modifier.padding(bottom = Spacing.small),
            )
        }
    }
}

@Composable
private fun FieldInput(
    field: TagField,
    draft: TagDraft,
    separator: String,
    onIntent: (EditorIntent) -> Unit,
) {
    val label = fieldLabel(field.key)
    val changed = draft.isChanged(field.key)
    val revert = { onIntent(EditorIntent.Revert(listOf(field.key))) }
    val modifier = Modifier.padding(bottom = Spacing.small)
    if (field.multiValue) {
        TagChipsField(
            label = label,
            values = draft.tags[field.key],
            onValuesChange = { onIntent(EditorIntent.SetField(field.key, it)) },
            changed = changed,
            onRevert = revert,
            separator = separator,
            modifier = modifier,
        )
    } else {
        TagTextField(
            label = label,
            value = draft.tags.first(field.key).orEmpty(),
            onValueChange = { onIntent(EditorIntent.SetField(field.key, listOf(it))) },
            changed = changed,
            onRevert = revert,
            singleLine = field.kind != FieldKind.LongText,
            maxLines = if (field.kind == FieldKind.LongText) 6 else 1,
            placeholder =
                if (field.kind == FieldKind.Date) stringResource(CoreUiR.string.field_date_hint)
                else null,
            keyboardType =
                if (field.kind == FieldKind.Number) KeyboardType.Number else KeyboardType.Text,
            capitalization =
                if (field == TagField.Isrc) KeyboardCapitalization.Characters
                else KeyboardCapitalization.Sentences,
            modifier = modifier,
        )
    }
}

@Composable
private fun AddFieldDialog(onAdd: (key: String, value: String) -> Unit, onDismiss: () -> Unit) {
    var key by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    val normalized = key.trim().uppercase().replace(' ', '_')
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_field)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.add_field_name)) },
                    placeholder = { Text(stringResource(R.string.add_field_name_hint)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.add_field_value)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(normalized, value.trim()) },
                enabled = normalized.isNotEmpty() && value.isNotBlank(),
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EditorProblem(
    status: EditorStatus,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(Spacing.extraLarge), contentAlignment = Alignment.Center) {
        when (status) {
            EditorStatus.NotFound ->
                PlaceholderCard(
                    title = stringResource(R.string.file_gone_title),
                    description = stringResource(R.string.file_gone_description),
                    icon = Icons.Rounded.SearchOff,
                    isError = true,
                    actionText = stringResource(R.string.go_back),
                    onAction = onClose,
                )
            EditorStatus.AccessDenied ->
                PlaceholderCard(
                    title = stringResource(R.string.access_denied_title),
                    description = stringResource(R.string.access_denied_description),
                    icon = Icons.Rounded.Error,
                    isError = true,
                    actionText = stringResource(R.string.go_back),
                    onAction = onClose,
                )
            EditorStatus.Unsupported ->
                PlaceholderCard(
                    title = stringResource(R.string.unsupported_title),
                    description = stringResource(R.string.unsupported_description),
                    icon = Icons.Rounded.Error,
                    isError = true,
                    actionText = stringResource(R.string.go_back),
                    onAction = onClose,
                )
            is EditorStatus.Failed ->
                PlaceholderCard(
                    title = stringResource(R.string.failed_title),
                    description = status.message,
                    icon = Icons.Rounded.Error,
                    isError = true,
                    actionText = stringResource(R.string.retry),
                    onAction = onRetry,
                    secondaryActionText = stringResource(R.string.go_back),
                    onSecondaryAction = onClose,
                )
            else -> Unit
        }
    }
}
