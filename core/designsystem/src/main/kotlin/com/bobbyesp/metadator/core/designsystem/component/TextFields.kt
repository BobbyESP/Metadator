/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight

/**
 * The colors of a tonal field's container. What a field says with an outline elsewhere (it has the
 * focus, it holds something to look at, what is in it is wrong) it says here with its tone.
 */
@Immutable
class TonalFieldColors(
    val container: Color,
    val focusedContainer: Color,
    val emphasis: Color,
    val error: Color,
) {
    /** The container of a field in the given state. An error wins over an emphasis. */
    fun container(focused: Boolean, emphasized: Boolean = false, isError: Boolean = false): Color {
        val base = if (focused) focusedContainer else container
        return when {
            isError -> lerp(base, error, TonalFieldDefaults.ERROR_TINT)
            emphasized -> lerp(base, emphasis, TonalFieldDefaults.EMPHASIS_TINT)
            else -> base
        }
    }
}

object TonalFieldDefaults {
    /**
     * How much of the emphasis color a field takes. Not all of it: after a lookup every field of a
     * song may be emphasized at once, and a screen of full containers would shout.
     */
    internal const val EMPHASIS_TINT = 0.55f
    internal const val ERROR_TINT = 0.5f

    val shape: Shape
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.large

    /**
     * For a field on the screen's own surface. One on something raised passes the two tones that
     * stand out from it: lower ones on a dialog, where the field reads as a well.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        container: Color = MaterialTheme.colorScheme.surfaceContainer,
        focusedContainer: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ): TonalFieldColors =
        TonalFieldColors(
            container = container,
            focusedContainer = focusedContainer,
            emphasis = MaterialTheme.colorScheme.primaryContainer,
            error = MaterialTheme.colorScheme.errorContainer,
        )

    /** [colors] for a field in a dialog, whose container is a high one. */
    @Composable
    @ReadOnlyComposable
    fun dialogColors(): TonalFieldColors =
        colors(
            container = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainer = MaterialTheme.colorScheme.surfaceContainerLowest,
        )

    /** [colors] for a field in a bottom sheet, whose container is a low one. */
    @Composable
    @ReadOnlyComposable
    fun sheetColors(): TonalFieldColors =
        colors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainer = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
}

/**
 * The app's text field: a rounded block of tone with the label inside it, and neither an outline
 * nor the line under Material's filled field. With many of them stacked, as in the editor, outlines
 * turn the screen into a grid of boxes; tone keeps them apart quietly.
 *
 * The field with the focus is a tone higher, and its label takes the primary color.
 *
 * @param emphasized tints the field with the primary color, to tell it from its neighbours (the
 *   editor: what will be written on saving)
 */
@Composable
fun TonalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    emphasized: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TonalFieldColors = TonalFieldDefaults.colors(),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = TonalFieldDefaults.shape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colors.container(focused = true, emphasized),
                unfocusedContainerColor = colors.container(focused = false, emphasized),
                disabledContainerColor = colors.container(focused = false),
                errorContainerColor = colors.container(focused = false, isError = true),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                unfocusedLabelColor =
                    if (emphasized) MaterialTheme.colorScheme.primary else Color.Unspecified,
            ),
    )
}
