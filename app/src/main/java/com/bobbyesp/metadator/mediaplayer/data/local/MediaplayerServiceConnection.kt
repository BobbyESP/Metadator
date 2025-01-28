package com.bobbyesp.metadator.mediaplayer.data.local

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaplayerService
import com.bobbyesp.utilities.Logging

@OptIn(UnstableApi::class)
class MediaplayerServiceConnection(
    private val connectionHandler: ConnectionHandler
) : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logging.i(
            "The Music Service is connected. Updating the connection handler."
        )
        val binder = service as MediaplayerService.MusicBinder
        connectionHandler.connect(binder.service.mediaServiceHandler)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logging.i(
            "The Music Service has been disconnected. Detaching the connection handler."
        )
        connectionHandler.disconnect()
    }
}