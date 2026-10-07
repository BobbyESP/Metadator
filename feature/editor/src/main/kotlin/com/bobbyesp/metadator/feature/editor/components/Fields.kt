/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.bobbyesp.metadator.core.designsystem.component.TonalFieldDefaults
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.ui.R as CoreUiR
import com.bobbyesp.metadator.feature.editor.R

/** The trailing "undo this change" button every changed field gets. */
@Composable
private fun RevertButton(label: String, onRevert: () -> Unit) {
    IconButton(onClick = onRevert, shapes = IconButtonDefaults.shapes()) {
        Icon(
            Icons.AutoMirrored.Rounded.Undo,
            contentDescription = stringResource(R.string.revert_field, label),
        )
    }
}

/**
 * A field with one value. A changed field takes the primary tint and an undo button, so what will
 * be written is visible at a glance before saving.
 */
@Composable
fun TagTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    changed: Boolean,
    onRevert: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    TonalTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth().changedState(changed),
        placeholder = placeholder,
        emphasized = changed,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon =
            if (changed) {
                { RevertButton(label, onRevert) }
            } else null,
        keyboardOptions =
            KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
            ),
    )
}

/**
 * Says that a field is changed to who cannot see its tint. Not a line of text under the field: that
 * moved everything below it down on the first key typed.
 */
@Composable
private fun Modifier.changedState(changed: Boolean): Modifier {
    if (!changed) return this
    val description = stringResource(R.string.changed)
    return semantics { stateDescription = description }
}

/**
 * A field with several values (artists, genres): one chip per value, so "Tyler, The Creator" is one
 * artist and not two. Typing and pressing enter adds a chip; tapping a chip edits it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipsField(
    label: String,
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
    changed: Boolean,
    onRevert: () -> Unit,
    separator: String,
    modifier: Modifier = Modifier,
) {
    val input = rememberTextFieldState()
    var focused by rememberSaveable(label) { mutableStateOf(false) }

    val commit = {
        val added = splitTyped(input.text.toString(), separator)
        if (added.isNotEmpty()) onValuesChange(values + added)
        input.clearText()
    }
    // Typing the separator ends a value, as a comma ends an email address.
    val latestCommit by rememberUpdatedState(commit)
    LaunchedEffect(input, separator) {
        val end = separator.trim()
        if (end.isEmpty()) return@LaunchedEffect
        snapshotFlow { input.text.toString() }
            .collect { typed ->
                if (typed.endsWith(end)) latestCommit()
            }
    }

    val container by
        animateColorAsState(
            targetValue = TonalFieldDefaults.colors().container(focused, emphasized = changed),
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ChipsFieldContainer",
        )
    Surface(
        modifier = modifier.fillMaxWidth().changedState(changed),
        shape = TonalFieldDefaults.shape,
        color = container,
    ) {
        Column(
            Modifier.padding(
                start = Spacing.large,
                end = Spacing.extraSmall,
                top = Spacing.small,
                bottom = Spacing.extraSmall,
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (focused || changed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (changed) RevertButton(label, onRevert)
            }
            if (values.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(end = Spacing.medium),
                ) {
                    values.forEachIndexed { index, value ->
                        InputChip(
                            selected = false,
                            onClick = {
                                // Editing a chip moves it back into the input.
                                commit()
                                input.setTextAndPlaceCursorAtEnd(value)
                                onValuesChange(values.filterIndexed { i, _ -> i != index })
                            },
                            label = { Text(value) },
                            shapes = InputChipDefaults.shapes(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        onValuesChange(values.filterIndexed { i, _ -> i != index })
                                    },
                                    modifier = Modifier.size(InputChipDefaults.AvatarSize),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription =
                                            stringResource(R.string.remove_value, value),
                                        modifier = Modifier.size(InputChipDefaults.IconSize),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            BasicTextField(
                state = input,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(end = Spacing.medium, top = Spacing.small, bottom = Spacing.medium)
                        .onFocusChanged {
                            if (focused && !it.isFocused) commit()
                            focused = it.isFocused
                        },
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                onKeyboardAction = { commit() },
                decorator = { inner ->
                    if (input.text.isEmpty()) {
                        Text(
                            stringResource(
                                if (values.isEmpty()) R.string.add_value_hint
                                else R.string.add_value
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                },
            )
        }
    }
}

private fun splitTyped(text: String, separator: String): List<String> {
    val trimmed = separator.trim()
    val parts = if (trimmed.isEmpty()) listOf(text) else text.split(trimmed)
    return parts.map { it.trim() }.filter { it.isNotEmpty() }
}

/** A position and its total, side by side: "Track 3 of 12". */
@Composable
fun PositionField(
    label: String,
    number: String,
    total: String,
    onChange: (number: String, total: String) -> Unit,
    changed: Boolean,
    onRevert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        TonalTextField(
            value = number,
            onValueChange = { onChange(it, total) },
            label = label,
            modifier = Modifier.weight(1f).changedState(changed),
            emphasized = changed,
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        TonalTextField(
            value = total,
            onValueChange = { onChange(number, it) },
            label = stringResource(CoreUiR.string.field_of),
            modifier = Modifier.weight(1f).changedState(changed),
            emphasized = changed,
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            trailingIcon =
                if (changed) {
                    { RevertButton(label, onRevert) }
                } else null,
        )
    }
}
