package com.bobbyesp.metadator.tageditor.presentation.pages.tageditor

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.bobbyesp.utilities.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSyncNeededDialog(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    val uriLauncher = LocalUriHandler.current
    AlertDialog(
        icon = {
            Icon(imageVector = Icons.Filled.Lyrics, contentDescription = "SongSync app icon")
        },
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(id = R.string.song_sync_needed)) },
        text = {
            Text(
                text =
                    buildAnnotatedString {
                        append(stringResource(id = R.string.song_sync_needed_desc))
                        append(" \n")
                        append(stringResource(id = R.string.song_sync_not_installed))
                    }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    uriLauncher.openUri("https://github.com/Lambada10/SongSync/releases/latest")
                }
            ) {
                Text(stringResource(id = R.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.dismiss)) }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SongSyncNeededDialogPreview() {
    SongSyncNeededDialog(onDismissRequest = {})
}
