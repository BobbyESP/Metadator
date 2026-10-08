/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.bobbyesp.metadator.core.designsystem.component.ActionMenu
import com.bobbyesp.metadator.core.designsystem.component.FloatingActionToolbar
import com.bobbyesp.metadator.core.designsystem.component.FloatingActionToolbarDefaults
import com.bobbyesp.metadator.core.designsystem.component.LabelChip
import com.bobbyesp.metadator.core.designsystem.component.LoadingScreen
import com.bobbyesp.metadator.core.designsystem.component.MenuAction
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.component.RolledIn
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.component.ShapedIcon
import com.bobbyesp.metadator.core.designsystem.component.TonalFieldDefaults
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.component.sidesOf
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.blurHalo
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
    val haze = rememberHazeState()
    val focusManager = LocalFocusManager.current

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
                    Modifier.fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .sidesOf(padding)
                        .pointerInput(focusManager) {
                            detectTapGestures { focusManager.clearFocus() }
                        }
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

                    BoxWithConstraints(Modifier.fillMaxSize().imePadding().hazeSource(haze)) {
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
                                .padding(bottom = Spacing.large)
                                .blurHalo(haze, FloatingActionToolbarDefaults.HaloShape),
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
    scrollBehavior: TopAppBarScrollBehavior,
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
                ActionMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    actions =
                        listOf(
                            MenuAction(
                                stringResource(R.string.revert_all),
                                Icons.AutoMirrored.Rounded.Undo,
                                enabled = canRevert,
                                onClick = onRevertAll,
                            )
                        ),
                )
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
    val savingLabel = stringResource(R.string.saving)
    FloatingActionToolbar(
        onPrimaryAction = { if (dirty && !saving) onSave() },
        primaryAction = {
            if (saving) {
                LoadingIndicator(
                    modifier = Modifier.semantics { contentDescription = savingLabel },
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            } else {
                Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.save))
            }
        },
        modifier = modifier,
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

/** Every editable section, as items of the editor's list. Each is a group, as the app's lists. */
private fun LazyListScope.fields(
    state: EditorState,
    draft: TagDraft,
    onIntent: (EditorIntent) -> Unit,
) {
    val separator = state.settings.multiValueSeparator
    val missing = draft.missingEssentials()
    if (missing.isNotEmpty()) {
        item(key = "attention") { AttentionNotice(missing, Modifier.animateItem()) }
    }
    item(key = "main-header") { SectionHeader(stringResource(R.string.section_main)) }
    fieldGroup(MainFields, draft, separator, onIntent)

    item(key = "track-header") { SectionHeader(stringResource(R.string.section_track)) }
    val positions = listOf(TagField.TrackNumber, TagField.DiscNumber)
    positions.forEachIndexed { index, field ->
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
                modifier = GroupGap,
                first = index == 0,
                last = index == positions.lastIndex,
            )
        }
    }

    item(key = "credits-header") { SectionHeader(stringResource(R.string.section_credits)) }
    fieldGroup(CreditFields, draft, separator, onIntent)

    item(key = "more-header") { SectionHeader(stringResource(R.string.section_more)) }
    fieldGroup(MoreFields, draft, separator, onIntent)

    val lyrics = draft.tags.first(TagField.Lyrics.key).orEmpty()
    item(key = "lyrics-header") {
        SectionHeader(
            stringResource(R.string.section_lyrics),
            trailing = {
                FilledTonalButton(
                    onClick = { onIntent(EditorIntent.FindLyrics) },
                    enabled = !state.findingLyrics,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.Lyrics, null, Modifier.size(ButtonDefaults.IconSize))
                    Text(
                        stringResource(R.string.lyrics_find),
                        Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            },
        )
    }
    if (Lrc.isSynced(lyrics)) {
        item(key = "lyrics-synced") {
            // Said, not pressed: a label, where a chip would ask to be tapped.
            Box(Modifier.padding(bottom = Spacing.small).animateItem()) {
                LabelChip(stringResource(R.string.lyrics_synced), Icons.Rounded.Schedule)
            }
        }
    }
    item(key = TagField.Lyrics.key) {
        TagTextField(
            label = fieldLabel(TagField.Lyrics.key),
            value = lyrics,
            onValueChange = { onIntent(EditorIntent.SetField(TagField.Lyrics.key, listOf(it))) },
            changed = draft.isChanged(TagField.Lyrics.key),
            onRevert = { onIntent(EditorIntent.Revert(listOf(TagField.Lyrics.key))) },
            singleLine = false,
            minLines = 4,
            maxLines = 14,
        )
    }

    val otherKeys =
        (draft.tags.keys + draft.original.tags.keys).filter { it !in KnownKeys }.distinct().sorted()
    item(key = "all-header") {
        var adding by rememberSaveable { mutableStateOf(false) }
        SectionHeader(
            stringResource(R.string.section_all_tags),
            trailing = {
                FilledTonalIconButton(
                    onClick = { adding = true },
                    shapes = IconButtonDefaults.shapes(),
                ) {
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
    otherKeys.forEachIndexed { index, key ->
        item(key = "raw:$key") {
            TagChipsField(
                label = key,
                values = draft.tags[key],
                onValuesChange = { onIntent(EditorIntent.SetField(key, it)) },
                changed = draft.isChanged(key),
                onRevert = { onIntent(EditorIntent.Revert(listOf(key))) },
                separator = separator,
                // No placement animation: a focused field stays composed out of view, and scrolled
                // back to, it would travel to its place from wherever it was kept.
                modifier = GroupGap.animateItem(placementSpec = null),
                shape = GroupShapes.itemShape(index, otherKeys.size),
            )
        }
    }
}

/**
 * The essential tags the song would still be missing if it were saved now: what the library flags
 * it for, read from the draft, so the notice goes as the fields are filled in.
 */
private fun TagDraft.missingEssentials(): List<TagField> =
    EssentialFields.keys.filter { field -> tags[field.key].all { it.isBlank() } }

/** The tags a song needs, in the order the editor has them, each with the icon of its chip. */
private val EssentialFields: Map<TagField, ImageVector> =
    linkedMapOf(
        TagField.Artist to Icons.Rounded.Person,
        TagField.Album to Icons.Rounded.Album,
        TagField.AlbumArtist to Icons.Rounded.Groups,
        TagField.Date to Icons.Rounded.CalendarMonth,
        TagField.TrackNumber to Icons.Rounded.Numbers,
    )

private const val PILL_FOLLOW_MS = 60

/** Why the library says this song needs attention, before the fields that would fix it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AttentionNotice(missing: List<TagField>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            // Read as one thing: "Needs attention. Year".
            modifier = Modifier.padding(Spacing.large).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShapedIcon(
                icon = Icons.Rounded.PriorityHigh,
                size = 56.dp,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                turnMillis = 20_000,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(
                    stringResource(R.string.needs_attention_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )

                var arrived by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) { arrived = true }

                FlowRow(Modifier.heightIn(min = AssistChipDefaults.Height + Spacing.small)) {
                    EssentialFields.entries.forEachIndexed { index, (field, icon) ->
                        RolledIn(
                            target = field.takeIf { arrived && it in missing },
                            delayMillis = index * PILL_FOLLOW_MS,
                        ) { shown ->
                            LabelChip(
                                label = fieldLabel(shown.key),
                                icon = icon,
                                modifier =
                                    Modifier.padding(end = Spacing.small, bottom = Spacing.small),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What keeps the fields of a group apart: the gap of the app's grouped lists. */
private val GroupGap = Modifier.padding(bottom = ListItemDefaults.SegmentedGap)

private fun LazyListScope.fieldGroup(
    fields: List<TagField>,
    draft: TagDraft,
    separator: String,
    onIntent: (EditorIntent) -> Unit,
) {
    fields.forEachIndexed { index, field ->
        item(key = field.key) {
            FieldInput(field, draft, separator, GroupShapes.itemShape(index, fields.size), onIntent)
        }
    }
}

@Composable
private fun FieldInput(
    field: TagField,
    draft: TagDraft,
    separator: String,
    shape: RoundedCornerShape,
    onIntent: (EditorIntent) -> Unit,
) {
    val label = fieldLabel(field.key)
    val changed = draft.isChanged(field.key)
    val revert = { onIntent(EditorIntent.Revert(listOf(field.key))) }
    if (field.multiValue) {
        TagChipsField(
            label = label,
            values = draft.tags[field.key],
            onValuesChange = { onIntent(EditorIntent.SetField(field.key, it)) },
            changed = changed,
            onRevert = revert,
            separator = separator,
            modifier = GroupGap,
            shape = shape,
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
            modifier = GroupGap,
            shape = shape,
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
                TonalTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = stringResource(R.string.add_field_name),
                    placeholder = stringResource(R.string.add_field_name_hint),
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next,
                        ),
                    colors = TonalFieldDefaults.dialogColors(),
                )
                TonalTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = stringResource(R.string.add_field_value),
                    colors = TonalFieldDefaults.dialogColors(),
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
