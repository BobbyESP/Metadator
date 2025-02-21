package com.bobbyesp.utilities.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bobbyesp.utilities.R
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun PermissionNotGrantedDialog(
    modifier: Modifier = Modifier,
    neededPermissions: PersistentList<PermissionType>,
        shouldShowRationale: Boolean = false,
    onGrantRequest: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(icon = {
        Icon(
            imageVector = Icons.Outlined.NotInterested,
            contentDescription = "Permission not granted"
        )
    }, modifier = modifier, onDismissRequest = onDismissRequest, title = {
        Text(text = stringResource(id = R.string.permission_not_granted))
    }, text = {
        Column {
            if (shouldShowRationale) {
                Text(
                    text = stringResource(id = R.string.permission_not_granted_rationale_desc),
                    textAlign = TextAlign.Justify
                )
            } else {
                Text(
                    text = stringResource(id = R.string.permission_not_granted_description),
                    textAlign = TextAlign.Justify
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(text = stringResource(id = R.string.permissions_to_grant))

            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                neededPermissions.forEach {
                    TextWithDot(text = it.toPermissionString())
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = onGrantRequest
        ) {
            Text(stringResource(id = R.string.grant))
        }
    }, dismissButton = {
        TextButton(
            onClick = onDismissRequest
        ) {
            Text(stringResource(id = R.string.dismiss))
        }
    })
}

@Composable
fun TextWithDot(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Immutable
enum class PermissionType(val permission: String) {
    READ_EXTERNAL_STORAGE(android.Manifest.permission.READ_EXTERNAL_STORAGE),
    WRITE_EXTERNAL_STORAGE(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
    READ_MEDIA_AUDIO(android.Manifest.permission.READ_MEDIA_AUDIO),
    INTERNET(android.Manifest.permission.INTERNET),
    ACCESS_NETWORK_STATE(android.Manifest.permission.ACCESS_NETWORK_STATE),
    ACCESS_WIFI_STATE(android.Manifest.permission.ACCESS_WIFI_STATE),
    CHANGE_WIFI_STATE(android.Manifest.permission.CHANGE_WIFI_STATE),
    MANAGE_EXTERNAL_STORAGE(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE);

    @Composable
    fun toPermissionString(): String {
        return when (this) {
            READ_EXTERNAL_STORAGE -> stringResource(R.string.read_external_storage)
            WRITE_EXTERNAL_STORAGE -> stringResource(R.string.write_external_storage)
            INTERNET -> stringResource(R.string.internet)
            ACCESS_NETWORK_STATE -> stringResource(R.string.access_network_state)
            ACCESS_WIFI_STATE -> stringResource(R.string.access_wifi_state)
            CHANGE_WIFI_STATE -> stringResource(R.string.change_wifi_state)
            READ_MEDIA_AUDIO -> stringResource(R.string.read_media_audio)
            MANAGE_EXTERNAL_STORAGE -> stringResource(R.string.manage_external_storage)
        }
    }

    @Composable
    fun toPermissionDescription(): String {
        return when (this) {
            READ_EXTERNAL_STORAGE -> stringResource(R.string.read_external_storage_description)
            WRITE_EXTERNAL_STORAGE -> stringResource(R.string.write_external_storage_description)
            READ_MEDIA_AUDIO -> stringResource(R.string.read_media_audio_description)
            else -> stringResource(R.string.unknown_permission_description)
        }
    }

    @Composable
    fun toPermissionIcon(): androidx.compose.ui.graphics.vector.ImageVector {
        return when (this) {
            READ_EXTERNAL_STORAGE -> Icons.Outlined.Folder
            WRITE_EXTERNAL_STORAGE -> Icons.Outlined.Folder
            MANAGE_EXTERNAL_STORAGE -> Icons.Outlined.Folder
            READ_MEDIA_AUDIO -> Icons.Outlined.MusicNote
            INTERNET -> Icons.Outlined.Wifi
            ACCESS_NETWORK_STATE -> Icons.Outlined.Wifi
            ACCESS_WIFI_STATE -> Icons.Outlined.Wifi
            CHANGE_WIFI_STATE -> Icons.Outlined.Wifi
        }
    }

    companion object {
        fun String.toPermissionType(): PermissionType {
            return when (this) {
                android.Manifest.permission.READ_EXTERNAL_STORAGE -> READ_EXTERNAL_STORAGE
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> WRITE_EXTERNAL_STORAGE
                android.Manifest.permission.INTERNET -> INTERNET
                android.Manifest.permission.ACCESS_NETWORK_STATE -> ACCESS_NETWORK_STATE
                android.Manifest.permission.ACCESS_WIFI_STATE -> ACCESS_WIFI_STATE
                android.Manifest.permission.CHANGE_WIFI_STATE -> CHANGE_WIFI_STATE
                android.Manifest.permission.READ_MEDIA_AUDIO -> READ_MEDIA_AUDIO
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> MANAGE_EXTERNAL_STORAGE
                else -> throw IllegalArgumentException("Unknown permission string")
            }
        }
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PermissionNotGrantedPreview() {
    MaterialTheme {
        PermissionNotGrantedDialog(
            onGrantRequest = {},
            onDismissRequest = {},
            neededPermissions = persistentListOf(
                PermissionType.READ_EXTERNAL_STORAGE, PermissionType.WRITE_EXTERNAL_STORAGE
            )
        )
    }
}