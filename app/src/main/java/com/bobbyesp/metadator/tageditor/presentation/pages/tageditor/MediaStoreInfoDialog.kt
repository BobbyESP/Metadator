package com.bobbyesp.metadator.tageditor.presentation.pages.tageditor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.R

@Composable
fun MediaStoreInfoDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        text = {
            Column {
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(id = R.string.dismiss))
            }
        }
    )
}