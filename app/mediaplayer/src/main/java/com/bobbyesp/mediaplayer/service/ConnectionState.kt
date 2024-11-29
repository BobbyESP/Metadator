package com.bobbyesp.mediaplayer.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(UnstableApi::class)
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connected(val serviceHandler: MediaServiceHandler) : ConnectionState()
}

@OptIn(UnstableApi::class)
class ConnectionHandler {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
        .onEach { newState ->
            Log.d("ConnectionHandler", "Connection state changed: $newState")
        }
        .stateIn(
            CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    fun connect(serviceHandler: MediaServiceHandler) {
        _connectionState.update {
            ConnectionState.Connected(serviceHandler)
        }
    }

    fun disconnect() {
        _connectionState.update {
            ConnectionState.Disconnected
        }
    }
}