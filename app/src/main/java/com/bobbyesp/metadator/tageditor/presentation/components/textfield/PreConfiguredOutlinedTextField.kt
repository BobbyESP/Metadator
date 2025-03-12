package com.bobbyesp.metadator.tageditor.presentation.components.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.bobbyesp.ui.R
import com.bobbyesp.ui.util.rememberVolatileSaveable
import com.materialkolor.DynamicMaterialTheme

@Composable
fun PreConfiguredOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String?,
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = 2,
    minLines: Int = 1,
    returnModifiedValue: (String) -> Unit = {},
) {
    val (text, setText) = rememberVolatileSaveable(value ?: "")
    val originalValue = remember { value ?: "" }

    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = { newValue ->
            setText(newValue)
            returnModifiedValue(newValue)
        },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = MaterialTheme.shapes.medium,
        trailingIcon = {
            AnimatedVisibility(
                visible = text != originalValue,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                IconButton(
                    onClick = {
                        setText(originalValue)
                        returnModifiedValue(originalValue)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(id = R.string.undo),
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun PreConfiguredOutlineTextFieldPreview() {
    DynamicMaterialTheme(seedColor = Color(0xFF4565FF)) {
        PreConfiguredOutlinedTextField(
            value = "Hello, World!",
            label = "Label",
            returnModifiedValue = {}
        )
    }
}
