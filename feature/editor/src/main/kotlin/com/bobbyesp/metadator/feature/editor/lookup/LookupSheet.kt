/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.lookup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.common.formatDuration
import com.bobbyesp.metadator.core.designsystem.component.ItemIcon
import com.bobbyesp.metadator.core.designsystem.component.LabelChip
import com.bobbyesp.metadator.core.designsystem.component.SectionHeader
import com.bobbyesp.metadator.core.designsystem.component.ShapedIcon
import com.bobbyesp.metadator.core.designsystem.component.ToggleChip
import com.bobbyesp.metadator.core.designsystem.component.TonalFieldDefaults
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.lookup.FieldProposal
import com.bobbyesp.metadator.core.ui.R as CoreUiR
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.core.ui.component.fieldLabel
import com.bobbyesp.metadator.feature.editor.R
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.MatchConfidence

/**
 * "Find metadata": search the online sources, pick a result, choose field by field what to use.
 * Applying fills the editor; nothing is written until the user saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupSheet(
    state: LookupState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (LookupCandidate) -> Unit,
    onToggle: (String) -> Unit,
    onCheckOnly: (Set<String>) -> Unit,
    onToggleCover: () -> Unit,
    onBackToResults: () -> Unit,
    onApply: (fields: Map<String, List<String>>, coverUrl: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = SheetValues)
    val motion = MaterialTheme.motionScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Keyed by the stage: ticking a field makes a new comparison, which updates in place.
        AnimatedContent(
            targetState = state.comparison,
            contentKey = { it != null },
            transitionSpec = {
                val direction = if (targetState != null) 1 else -1
                (slideInHorizontally(motion.defaultSpatialSpec()) { direction * it / 4 } +
                    fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motion.defaultSpatialSpec()) { -direction * it / 4 } +
                        fadeOut(motion.fastEffectsSpec()))
            },
            label = "LookupStage",
        ) { comparison ->
            if (comparison == null) {
                SearchStage(state, onTitleChange, onArtistChange, onAlbumChange, onSearch, onOpen)
            } else {
                ComparisonStage(
                    comparison = comparison,
                    onToggle = onToggle,
                    onCheckOnly = onCheckOnly,
                    onToggleCover = onToggleCover,
                    onBack = onBackToResults,
                    onApply = onApply,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val SheetValues = setOf(SheetValue.Hidden, SheetValue.Expanded)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchStage(
    state: LookupState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (LookupCandidate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
        contentPadding =
            PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.huge),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "query", contentType = "query") {
            QueryForm(
                title = state.title,
                artist = state.artist,
                album = state.album,
                searching = state.status == LookupStatus.Searching,
                onTitleChange = onTitleChange,
                onArtistChange = onArtistChange,
                onAlbumChange = onAlbumChange,
                onSearch = onSearch,
            )
        }
        when (val status = state.status) {
            LookupStatus.Idle -> Unit
            LookupStatus.Searching ->
                item(key = "searching", contentType = "searching") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.huge).animateItem(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        ContainedLoadingIndicator()
                        Text(
                            stringResource(R.string.lookup_searching),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            is LookupStatus.Results -> {
                if (status.candidates.isEmpty()) {
                    item(key = "state", contentType = "state") {
                        StateMessage(
                            Icons.Rounded.SearchOff,
                            stringResource(R.string.lookup_no_results),
                            Modifier.animateItem(),
                        )
                    }
                } else {
                    item(key = "results", contentType = "results") {
                        SectionHeader(
                            stringResource(R.string.lookup_results),
                            Modifier.animateItem(),
                        )
                    }
                }
                itemsIndexed(
                    status.candidates,
                    key = { _, it -> it.providerId + it.id },
                    contentType = { _, _ -> "candidate" },
                ) { index, candidate ->
                    CandidateItem(
                        candidate,
                        index,
                        status.candidates.size,
                        Modifier.animateItem(),
                    ) {
                        onOpen(candidate)
                    }
                }
                if (status.failedProviders.isNotEmpty()) {
                    item(key = "partial", contentType = "partial") {
                        Text(
                            stringResource(
                                R.string.lookup_partial,
                                status.failedProviders.joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier.fillMaxWidth().padding(top = Spacing.medium).animateItem(),
                        )
                    }
                }
            }
            LookupStatus.NoProviders ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.TravelExplore,
                        stringResource(R.string.lookup_no_providers),
                        Modifier.animateItem(),
                    )
                }
            LookupStatus.Offline ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.CloudOff,
                        stringResource(R.string.offline),
                        Modifier.animateItem(),
                        isError = true,
                    )
                }
            LookupStatus.RateLimited ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.HourglassTop,
                        stringResource(R.string.lookup_rate_limited),
                        Modifier.animateItem(),
                    )
                }
            LookupStatus.Failed ->
                item(key = "state", contentType = "state") {
                    StateMessage(
                        Icons.Rounded.ErrorOutline,
                        stringResource(R.string.lookup_failed),
                        Modifier.animateItem(),
                        isError = true,
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QueryForm(
    title: String,
    artist: String,
    album: String,
    searching: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            stringResource(R.string.lookup_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
        QueryField(CoreUiR.string.field_title, title, onChange = onTitleChange)
        QueryField(CoreUiR.string.field_artist, artist, onChange = onArtistChange)
        QueryField(CoreUiR.string.field_album, album, onSearch, onAlbumChange)
        SheetButton(
            text = stringResource(R.string.lookup_search),
            icon = Icons.Rounded.Search,
            onClick = onSearch,
            enabled = !searching,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
        )
    }
}

@Composable
private fun QueryField(
    label: Int,
    value: String,
    onDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    TonalTextField(
        value = value,
        onValueChange = onChange,
        label = stringResource(label),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(imeAction = if (onDone != null) ImeAction.Search else ImeAction.Next),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        colors = TonalFieldDefaults.sheetColors(),
    )
}

/**
 * The one thing to do at each stage, as Material's medium button: tall and as wide as the sheet, so
 * that it is found without being looked for.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
    ) {
        Icon(icon, null, Modifier.size(ButtonDefaults.iconSizeFor(height)))
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        Text(text, style = ButtonDefaults.textStyleFor(height))
    }
}

/** Why there are no results, under the shape the app's empty screens use. */
@Composable
private fun StateMessage(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        ShapedIcon(
            icon = icon,
            size = 88.dp,
            containerColor = if (isError) colors.errorContainer else colors.primaryContainer,
            contentColor = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CandidateItem(
    candidate: LookupCandidate,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = GroupShapes.listItemShapes(index, count),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        colors =
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        leadingContent = {
            ArtworkImage(
                model = candidate.artworkThumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        },
        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(candidate.summary(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                CandidateLabels(candidate)
            }
        },
    ) {
        Text(
            candidate.title,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LookupCandidate.summary(): String =
    listOfNotNull(
            artists.joinToString(", ").ifEmpty { null },
            album,
            year,
            durationMs?.let(::formatDuration),
        )
        .joinToString(" · ")

/** How good a match it is and where it comes from: said, not pressed, so labels and not chips. */
@Composable
private fun CandidateLabels(candidate: LookupCandidate, modifier: Modifier = Modifier) {
    val confidence = MatchConfidence.of(candidate.score)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        LabelChip(
            label =
                stringResource(
                    when (confidence) {
                        MatchConfidence.High -> R.string.lookup_confidence_high
                        MatchConfidence.Medium -> R.string.lookup_confidence_medium
                        MatchConfidence.Low -> R.string.lookup_confidence_low
                    }
                ),
            icon =
                when (confidence) {
                    MatchConfidence.High -> Icons.Rounded.Verified
                    MatchConfidence.Medium -> Icons.AutoMirrored.Rounded.HelpOutline
                    MatchConfidence.Low -> Icons.Rounded.WarningAmber
                },
        )
        LabelChip(label = candidate.providerName, icon = Icons.Rounded.Public)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ComparisonStage(
    comparison: Comparison,
    onToggle: (String) -> Unit,
    onCheckOnly: (Set<String>) -> Unit,
    onToggleCover: () -> Unit,
    onBack: () -> Unit,
    onApply: (fields: Map<String, List<String>>, coverUrl: String?) -> Unit,
) {
    var showUnchanged by rememberSaveable { mutableStateOf(false) }
    val proposals = comparison.proposals
    val unchangedCount = remember(proposals) { proposals.count { !it.differs } }
    val shown =
        remember(proposals, showUnchanged) {
            if (showUnchanged) proposals else proposals.filter { it.differs }
        }
    val shownKeys = remember(shown) { shown.mapTo(mutableSetOf()) { it.key } }
    val checkedShown = shownKeys.count { it in comparison.checked }
    val candidate = comparison.candidate
    val hasCover = candidate.artworkUrl != null
    val latest by rememberUpdatedState(comparison)

    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "title", contentType = "title") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.small),
                ) {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.lookup_back),
                        )
                    }
                    Text(
                        stringResource(R.string.lookup_compare_title),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        modifier = Modifier.padding(start = Spacing.extraSmall),
                    )
                }
            }
            item(key = "candidate", contentType = "candidate") {
                CandidateHero(candidate, GroupShapes.itemShape(0, if (hasCover) 2 else 1))
            }
            if (hasCover) {
                item(key = "cover", contentType = "cover") {
                    SegmentedListItem(
                        checked = comparison.includeCover,
                        onCheckedChange = { onToggleCover() },
                        shapes = GroupShapes.listItemShapes(1, 2),
                        verticalAlignment = Alignment.CenterVertically,
                        colors =
                            ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        leadingContent = { ItemIcon(Icons.Rounded.Image) },
                        trailingContent = {
                            Checkbox(checked = comparison.includeCover, onCheckedChange = null)
                        },
                    ) {
                        Text(
                            stringResource(R.string.lookup_use_cover),
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                        )
                    }
                }
            }
            if (shown.isNotEmpty()) {
                item(key = "fields", contentType = "fields") {
                    val allChecked = checkedShown == shown.size
                    SectionHeader(
                        stringResource(R.string.lookup_selected_count, checkedShown, shown.size),
                        Modifier.animateItem(),
                    ) {
                        TextButton(
                            onClick = { onCheckOnly(if (allChecked) emptySet() else shownKeys) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(
                                stringResource(
                                    if (allChecked) R.string.lookup_select_none
                                    else R.string.lookup_select_all
                                )
                            )
                        }
                    }
                }
            }
            itemsIndexed(
                shown,
                key = { _, it -> "field:" + it.key },
                contentType = { _, _ -> "field" },
            ) { index, proposal ->
                ProposalItem(
                    proposal,
                    proposal.key in comparison.checked,
                    index,
                    shown.size,
                    Modifier.animateItem(),
                ) {
                    onToggle(proposal.key)
                }
            }
            if (unchangedCount > 0) {
                item(key = "unchanged", contentType = "unchanged") {
                    ToggleChip(
                        selected = showUnchanged,
                        onClick = { showUnchanged = !showUnchanged },
                        label = stringResource(R.string.lookup_show_unchanged, unchangedCount),
                        modifier = Modifier.padding(top = Spacing.small).animateItem(),
                    )
                }
            }
        }
        SheetButton(
            text = stringResource(R.string.lookup_apply),
            icon = Icons.Rounded.Check,
            onClick = {
                onApply(
                    latest.selectedFields,
                    latest.candidate.artworkUrl.takeIf { latest.includeCover },
                )
            },
            enabled = comparison.checked.isNotEmpty() || comparison.includeCover,
            modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
        )
    }
}

/** The result being compared, said once and large: what the fields below would make of the song. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CandidateHero(candidate: LookupCandidate, shape: Shape, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                model = candidate.artworkThumbnailUrl ?: candidate.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.large,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    candidate.title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.summary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                CandidateLabels(candidate, Modifier.padding(top = Spacing.extraSmall))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProposalItem(
    proposal: FieldProposal,
    checked: Boolean,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val empty = stringResource(R.string.lookup_empty_value)
    SegmentedListItem(
        checked = checked,
        onCheckedChange = { onToggle() },
        shapes = GroupShapes.listItemShapes(index, count),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        colors =
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        overlineContent = { Text(fieldLabel(proposal.key)) },
        supportingContent =
            if (proposal.differs) {
                {
                    Text(
                        stringResource(
                            R.string.lookup_current,
                            proposal.current.joinToString(", ").ifEmpty { empty },
                        )
                    )
                }
            } else null,
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
    ) {
        Text(
            proposal.proposed.joinToString(", "),
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
    }
}
