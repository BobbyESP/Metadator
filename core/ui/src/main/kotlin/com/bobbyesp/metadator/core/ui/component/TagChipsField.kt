/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bobbyesp.metadator.core.designsystem.component.TonalFieldDefaults
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.ui.R

/**
 * The trailing "undo this change" button every changed field gets.
 *
 * @param description what it undoes, for who cannot see it
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RevertFieldButton(
    label: String,
    onRevert: () -> Unit,
    description: String = stringResource(R.string.field_revert, label),
) {
    IconButton(onClick = onRevert, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = description)
    }
}

/** Says that a field is changed to who cannot see its tint. */
@Composable
fun Modifier.changedState(changed: Boolean): Modifier {
    if (!changed) return this
    val description = stringResource(R.string.field_changed)
    return semantics { stateDescription = description }
}

/**
 * A field with several values (artists, genres): one chip per value, so "Tyler, The Creator" is one
 * artist and not two. Typing and pressing enter adds a chip; tapping a chip edits it.
 *
 * @param changed whether it holds something to be written, which tints it and gives it its undo
 * @param separator what ends a value as it is typed, as enter does
 * @param placeholder said in place of the values while there are none, where their absence means
 *   something (the batch editor: the songs differ)
 * @param revertDescription what the undo button does, for who cannot see it
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    placeholder: String? = null,
    revertDescription: String = stringResource(R.string.field_revert, label),
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
    val resting = !focused && values.isEmpty() && inputEmpty && placeholder == null
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
                                                stringResource(R.string.field_remove_value, value),
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
                                if (inputEmpty && !resting) {
                                    Text(
                                        when {
                                            values.isNotEmpty() ->
                                                stringResource(R.string.field_add_value)
                                            placeholder != null && !focused -> placeholder
                                            else -> stringResource(R.string.field_add_value_hint)
                                        },
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
                    RevertFieldButton(label, onRevert, revertDescription)
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
