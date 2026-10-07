/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.lookup

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.common.formatDuration
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
    onQueryChange: (title: String, artist: String, album: String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (LookupCandidate) -> Unit,
    onToggle: (String) -> Unit,
    onToggleCover: () -> Unit,
    onBackToResults: () -> Unit,
    onApply: (fields: Map<String, List<String>>, coverUrl: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // No half-open state: the sheet is as tall as what it shows.
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AnimatedContent(targetState = state.comparison, label = "LookupStage") { comparison ->
            if (comparison == null) {
                SearchStage(state, onQueryChange, onSearch, onOpen)
            } else {
                ComparisonStage(
                    comparison = comparison,
                    onToggle = onToggle,
                    onToggleCover = onToggleCover,
                    onBack = onBackToResults,
                    onApply = {
                        onApply(
                            comparison.selectedFields,
                            comparison.candidate.artworkUrl.takeIf { comparison.includeCover },
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchStage(
    state: LookupState,
    onQueryChange: (String, String, String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (LookupCandidate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
        contentPadding =
            PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.huge),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.padding(bottom = Spacing.medium),
            ) {
                Text(
                    stringResource(R.string.lookup_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                QueryField(CoreUiR.string.field_title, state.title) {
                    onQueryChange(it, state.artist, state.album)
                }
                QueryField(CoreUiR.string.field_artist, state.artist) {
                    onQueryChange(state.title, it, state.album)
                }
                QueryField(CoreUiR.string.field_album, state.album, onSearch) {
                    onQueryChange(state.title, state.artist, it)
                }
                Button(
                    onClick = onSearch,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.End),
                    enabled = state.status != LookupStatus.Searching,
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(ButtonDefaults.IconSize))
                    Text(
                        stringResource(R.string.lookup_search),
                        Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
        when (val status = state.status) {
            LookupStatus.Idle -> Unit
            LookupStatus.Searching ->
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(Spacing.huge),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
            is LookupStatus.Results -> {
                if (status.failedProviders.isNotEmpty()) {
                    item {
                        Message(
                            stringResource(
                                R.string.lookup_partial,
                                status.failedProviders.joinToString(", "),
                            )
                        )
                    }
                }
                if (status.candidates.isEmpty())
                    item { Message(stringResource(R.string.lookup_no_results)) }
                itemsIndexed(status.candidates, key = { _, it -> it.providerId + it.id }) {
                    index,
                    candidate ->
                    CandidateItem(candidate, index, status.candidates.size) { onOpen(candidate) }
                }
            }
            LookupStatus.NoProviders ->
                item { Message(stringResource(R.string.lookup_no_providers)) }
            LookupStatus.Offline -> item { Message(stringResource(R.string.offline)) }
            LookupStatus.RateLimited ->
                item { Message(stringResource(R.string.lookup_rate_limited)) }
            LookupStatus.Failed -> item { Message(stringResource(R.string.lookup_failed)) }
        }
    }
}

@Composable
private fun QueryField(
    label: Int,
    value: String,
    onDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        keyboardOptions =
            KeyboardOptions(imeAction = if (onDone != null) ImeAction.Search else ImeAction.Next),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
    )
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.large),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CandidateItem(candidate: LookupCandidate, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = GroupShapes.listItemShapes(index, count),
        verticalAlignment = Alignment.CenterVertically,
        colors =
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        leadingContent = {
            ArtworkImage(
                model = candidate.artworkThumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    listOfNotNull(
                            candidate.artists.joinToString(", ").ifEmpty { null },
                            candidate.album,
                            candidate.year,
                            candidate.durationMs?.let(::formatDuration),
                        )
                        .joinToString(" · "),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    SuggestionChip(
                        onClick = onClick,
                        label = { Text(confidenceLabel(candidate.score)) },
                    )
                    SuggestionChip(onClick = onClick, label = { Text(candidate.providerName) })
                }
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

@Composable
private fun confidenceLabel(score: Double): String =
    stringResource(
        when (MatchConfidence.of(score)) {
            MatchConfidence.High -> R.string.lookup_confidence_high
            MatchConfidence.Medium -> R.string.lookup_confidence_medium
            MatchConfidence.Low -> R.string.lookup_confidence_low
        }
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ComparisonStage(
    comparison: Comparison,
    onToggle: (String) -> Unit,
    onToggleCover: () -> Unit,
    onBack: () -> Unit,
    onApply: () -> Unit,
) {
    var showUnchanged by rememberSaveable { mutableStateOf(false) }
    val changed = comparison.changedProposals
    val unchanged = comparison.proposals.filterNot { it.differs }
    val shown = if (showUnchanged) comparison.proposals else changed
    val candidate = comparison.candidate

    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.lookup_compare_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            stringResource(R.string.lookup_from, candidate.providerName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (candidate.artworkUrl != null) {
                item {
                    SegmentedListItem(
                        checked = comparison.includeCover,
                        onCheckedChange = { onToggleCover() },
                        shapes = GroupShapes.listItemShapes(0, 1),
                        verticalAlignment = Alignment.CenterVertically,
                        colors =
                            ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        leadingContent = {
                            ArtworkImage(
                                candidate.artworkThumbnailUrl ?: candidate.artworkUrl,
                                null,
                                Modifier.size(72.dp),
                            )
                        },
                        trailingContent = {
                            Checkbox(checked = comparison.includeCover, onCheckedChange = null)
                        },
                    ) {
                        Text(stringResource(R.string.lookup_use_cover))
                    }
                }
                item { Box(Modifier.padding(top = Spacing.small)) }
            }
            itemsIndexed(shown, key = { _, it -> it.key }) { index, proposal ->
                ProposalItem(proposal, proposal.key in comparison.checked, index, shown.size) {
                    onToggle(proposal.key)
                }
            }
            if (unchanged.isNotEmpty() && !showUnchanged) {
                item {
                    TextButton(onClick = { showUnchanged = true }) {
                        Text(stringResource(R.string.lookup_show_unchanged, unchanged.size))
                    }
                }
            }
        }
        Button(
            onClick = onApply,
            shapes = ButtonDefaults.shapes(),
            enabled = comparison.checked.isNotEmpty() || comparison.includeCover,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(Spacing.screen)
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
            Text(stringResource(R.string.lookup_apply))
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
    onToggle: () -> Unit,
) {
    val empty = stringResource(R.string.lookup_empty_value)
    SegmentedListItem(
        checked = checked,
        onCheckedChange = { onToggle() },
        shapes = GroupShapes.listItemShapes(index, count),
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
