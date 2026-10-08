/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** The permission that lets the app list audio files. */
internal val AudioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

internal enum class PermissionStatus {
    Granted,
    /** Never asked, or asked once and refused: the system will still show its dialog. */
    Askable,
    /** Refused for good: only the app's settings page can grant it now. */
    PermanentlyDenied,
}

@Stable
internal class AudioPermissionState(
    status: PermissionStatus,
    private val onRequest: () -> Unit,
    private val onOpenSettings: () -> Unit,
) {
    var status by mutableStateOf(status)
        internal set

    fun request() =
        if (status == PermissionStatus.PermanentlyDenied) onOpenSettings() else onRequest()
}

/**
 * Tracks the audio permission, rechecking it every time the screen comes back: the user may have
 * granted it in the system settings meanwhile.
 */
@Composable
internal fun rememberAudioPermissionState(): AudioPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    lateinit var state: AudioPermissionState
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            askedOnce = true
            state.status = currentStatus(context, activity, askedOnce)
        }
    state = remember {
        AudioPermissionState(
            status = currentStatus(context, activity, askedOnce),
            onRequest = { launcher.launch(AudioPermission) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.status = currentStatus(context, activity, askedOnce)
    }
    return state
}

private fun currentStatus(
    context: Context,
    activity: Activity?,
    askedOnce: Boolean,
): PermissionStatus =
    when {
        ContextCompat.checkSelfPermission(context, AudioPermission) ==
            PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted
        // Refused, and the system no longer wants to explain why: it will not ask again.
        askedOnce && activity?.shouldShowRequestPermissionRationale(AudioPermission) == false ->
            PermissionStatus.PermanentlyDenied
        else -> PermissionStatus.Askable
    }
