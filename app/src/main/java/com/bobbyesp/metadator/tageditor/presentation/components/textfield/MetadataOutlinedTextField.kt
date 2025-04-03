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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.bobbyesp.ui.R
import com.bobbyesp.ui.util.rememberVolatileSaveable
import com.materialkolor.DynamicMaterialTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetadataOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String?,
    label: String = "",
    isModified: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = 2,
    minLines: Int = 1,
    onValueChange: (String) -> Unit = {},
) {
    val originalValue by remember { mutableStateOf(value ?: "") }
    var text by rememberSaveable { mutableStateOf(originalValue) }

    val fieldModified = text != originalValue || isModified

    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = { newValue ->
            text = newValue
            onValueChange(newValue)
        },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (fieldModified)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = if (fieldModified)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.outline
        ),
        trailingIcon = {
            AnimatedVisibility(
                visible = fieldModified,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                IconButton(
                    onClick = {
                        text = originalValue
                        onValueChange(originalValue)
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
        MetadataOutlinedTextField(
            value = "Hello, World!",
            label = "Label",
        )
    }
}
