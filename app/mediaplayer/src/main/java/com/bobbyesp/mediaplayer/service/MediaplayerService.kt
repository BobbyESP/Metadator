package com.bobbyesp.mediaplayer.service

import android.content.Intent
import android.os.Binder
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.bobbyesp.mediaplayer.service.notifications.MediaNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaplayerService : MediaSessionService() {
    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var notificationManager: MediaNotificationManager

    @Inject
    lateinit var connectionHandler: ConnectionHandler

    @Inject
    lateinit var mediaServiceHandler: MediaServiceHandler

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectionHandler.connect(mediaServiceHandler)
        notificationManager.startNotificationService(
            mediaSessionService = this,
            mediaSession = mediaSession
        )

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    inner class MusicBinder : Binder() {
        val service: MediaplayerService
            get() = this@MediaplayerService
    }
}