package com.bobbyesp.mediaplayer.service

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connected(val serviceHandler: MediaServiceHandler) : ConnectionState()
}

class ConnectionHandler {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    init {
        connectionState.onEach { newState ->
            Log.d("ConnectionHandler", "Connection state changed: $newState")
        }.launchIn(GlobalScope)
    }

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