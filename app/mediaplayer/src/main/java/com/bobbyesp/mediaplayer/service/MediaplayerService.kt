package com.bobbyesp.mediaplayer.service

import android.content.Intent
import android.os.Binder
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.bobbyesp.mediaplayer.service.notifications.MediaNotificationManager
import com.bobbyesp.mediaplayer.service.notifications.customLayout.MediaSessionLayoutHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaplayerService : MediaLibraryService(), MediaController.Listener {
    @Inject
    lateinit var mediaSession: MediaLibrarySession

    @Inject
    lateinit var mediaServiceHandler: MediaServiceHandler

    @Inject
    lateinit var notificationManager: MediaNotificationManager

    @Inject
    lateinit var mediaSessionLayoutHandler: MediaSessionLayoutHandler

    @Inject
    lateinit var connectionHandler: ConnectionHandler

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectionHandler.connect(mediaServiceHandler)
        notificationManager.startNotificationService(
            mediaSessionService = this, mediaSession = mediaSession
        )
        mediaServiceHandler.setMediaSessionInterface(mediaSessionLayoutHandler)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        connectionHandler.disconnect()
        mediaSession.run {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.clearMediaItems()
                player.playWhenReady = false
                player.stop()
            }
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

    inner class MusicBinder : Binder() {
        val service: MediaplayerService
            get() = this@MediaplayerService
    }
}