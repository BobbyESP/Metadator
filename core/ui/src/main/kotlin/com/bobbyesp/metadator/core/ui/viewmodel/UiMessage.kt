/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.viewmodel

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/** Something to tell the user, with at most one action (Undo, Retry). */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val long: Boolean = false,
    val onAction: (() -> Unit)? = null,
)

/** The app's one snackbar host, so messages outlive the screen that raised them. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/** Shows a ViewModel's messages while the screen is started. */
@Composable
fun ShowMessages(messages: Flow<UiMessage>) {
    val host = LocalSnackbarHostState.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(messages, host) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            messages.collect { message ->
                val result =
                    host.showSnackbar(
                        message = message.text,
                        actionLabel = message.actionLabel,
                        withDismissAction = message.actionLabel == null && message.long,
                        duration =
                            when {
                                message.actionLabel != null -> SnackbarDuration.Long
                                message.long -> SnackbarDuration.Long
                                else -> SnackbarDuration.Short
                            },
                    )
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
        }
    }
}

/** Collects one-off effects while the screen is started. */
@Composable
fun <T> Flow<T>.CollectEffects(vararg keys: Any?, onEffect: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val flow = this
    LaunchedEffect(flow, *keys) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect(onEffect) }
    }
}
