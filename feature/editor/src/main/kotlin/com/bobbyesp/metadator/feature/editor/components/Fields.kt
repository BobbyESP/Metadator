/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bobbyesp.metadator.core.designsystem.component.TonalFieldDefaults
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
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
    shape: RoundedCornerShape = GroupShapes.itemShape(0, 1),
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    TonalTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth().changedState(changed),
        shape = GroupShapes.focusedShape(shape, focused),
        interactionSource = interactions,
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
    shape: RoundedCornerShape = GroupShapes.itemShape(0, 1),
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

    val inputEmpty by remember(input) { derivedStateOf { input.text.isEmpty() } }
    val resting = !focused && values.isEmpty() && inputEmpty
    val lift by
        animateFloatAsState(
            targetValue = if (resting) 0f else 1f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "ChipsFieldLabel",
        )
    val typography = MaterialTheme.typography
    val restingScale = typography.bodyLarge.fontSize.value / typography.bodySmall.fontSize.value
    val restingDrop = with(LocalDensity.current) { typography.bodyLarge.lineHeight.toPx() / 2 }
    val focusRequester = remember { FocusRequester() }

    val container by
        animateColorAsState(
            targetValue = TonalFieldDefaults.colors().container(focused, emphasized = changed),
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ChipsFieldContainer",
        )
    Surface(
        modifier =
            modifier.fillMaxWidth().changedState(changed).pointerInput(focusRequester) {
                detectTapGestures { focusRequester.requestFocus() }
            },
        shape = GroupShapes.focusedShape(shape, focused),
        color = container,
    ) {
        Box {
            Column(
                Modifier.padding(
                    start = Spacing.large,
                    end = if (changed) RevertClearance else Spacing.medium,
                    top = Spacing.small,
                    bottom = if (values.isEmpty()) Spacing.small else Spacing.extraSmall,
                )
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (focused || changed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.graphicsLayer {
                            val rest = 1f - lift
                            val scale = lerp(1f, restingScale, rest)
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = scale
                            scaleY = scale
                            translationY = restingDrop * rest
                        },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
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
                                modifier = Modifier.height(ChipHeight),
                                shapes = InputChipDefaults.shapes(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            onValuesChange(
                                                values.filterIndexed { i, _ -> i != index }
                                            )
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
                    BasicTextField(
                        state = input,
                        modifier =
                            Modifier.focusRequester(focusRequester).onFocusChanged {
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
                            Box(
                                Modifier.heightIn(
                                    min = if (values.isEmpty()) Dp.Unspecified else ChipHeight
                                ),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (inputEmpty && (focused || values.isNotEmpty())) {
                                    Text(
                                        stringResource(
                                            if (values.isEmpty()) R.string.add_value_hint
                                            else R.string.add_value
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                            ),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
            if (changed) {
                Box(Modifier.align(Alignment.CenterEnd).padding(end = Spacing.extraSmall)) {
                    RevertButton(label, onRevert)
                }
            }
        }
    }
}

private val ChipHeight = 28.dp

/** The room the undo button takes at the end of a changed field. */
private val RevertClearance = 52.dp

private fun splitTyped(text: String, separator: String): List<String> {
    val trimmed = separator.trim()
    val parts = if (trimmed.isEmpty()) listOf(text) else text.split(trimmed)
    return parts.map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * A position and its total, side by side: "Track 3 of 12". Two cells of a group: [first] and [last]
 * say whether theirs is the group's first row and its last one.
 */
@Composable
fun PositionField(
    label: String,
    number: String,
    total: String,
    onChange: (number: String, total: String) -> Unit,
    changed: Boolean,
    onRevert: () -> Unit,
    modifier: Modifier = Modifier,
    first: Boolean = true,
    last: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        verticalAlignment = Alignment.Top,
    ) {
        PositionCell(
            value = number,
            onValueChange = { onChange(it, total) },
            label = label,
            changed = changed,
            shape = GroupShapes.cellShape(first, last, end = false),
            modifier = Modifier.weight(1f),
        )
        PositionCell(
            value = total,
            onValueChange = { onChange(number, it) },
            label = stringResource(CoreUiR.string.field_of),
            changed = changed,
            shape = GroupShapes.cellShape(first, last, start = false),
            modifier = Modifier.weight(1f),
            trailingIcon =
                if (changed) {
                    { RevertButton(label, onRevert) }
                } else null,
        )
    }
}

@Composable
private fun PositionCell(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    changed: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    TonalTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.changedState(changed),
        emphasized = changed,
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        trailingIcon = trailingIcon,
        shape = GroupShapes.focusedShape(shape, focused),
        interactionSource = interactions,
    )
}
