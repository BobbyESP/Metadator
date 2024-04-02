package com.bobbyesp.ui.components.text

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bobbyesp.ui.R

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
    maxLines: Int = 1,
    minLines: Int = 1,
    returnModifiedValue: (String) -> Unit = {}
) {
    val (text, setText) = remember(value) { mutableStateOf(value) }

    SideEffect {
        if (!text.isNullOrEmpty()) returnModifiedValue(text) else returnModifiedValue("")
    }

    OutlinedTextField(
        modifier = modifier,
        value = text ?: "",
        onValueChange = setText,
        label = { Text(text = label) },
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        trailingIcon = {
            AnimatedVisibility(
                visible = text != value,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                IconButton(onClick = {
                    setText(value)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(
                            id = R.string.undo
                        )
                    )
                }
            }
        }
    )

}