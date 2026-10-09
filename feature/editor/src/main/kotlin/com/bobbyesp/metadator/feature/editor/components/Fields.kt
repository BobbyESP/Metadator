/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.bobbyesp.metadator.core.designsystem.component.TonalTextField
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.ui.R as CoreUiR
import com.bobbyesp.metadator.core.ui.component.RevertFieldButton
import com.bobbyesp.metadator.core.ui.component.changedState

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
                { RevertFieldButton(label, onRevert) }
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
                    { RevertFieldButton(label, onRevert) }
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
