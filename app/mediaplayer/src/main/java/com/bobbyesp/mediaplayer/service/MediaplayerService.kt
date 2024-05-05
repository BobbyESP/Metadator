package com.bobbyesp.mediaplayer.service

import android.content.Intent
import android.os.Binder
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.bobbyesp.mediaplayer.R
import com.bobbyesp.mediaplayer.service.MediaServiceHandler.Companion.CommandToggleRepeatMode
import com.bobbyesp.mediaplayer.service.MediaServiceHandler.Companion.CommandToggleShuffle
import com.bobbyesp.mediaplayer.service.notifications.MediaNotificationManager
import com.bobbyesp.mediaplayer.service.notifications.MediaSessionLayoutHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaplayerService : MediaSessionService(), MediaSessionLayoutHandler {
    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var notificationManager: MediaNotificationManager

    @Inject
    lateinit var connectionHandler: ConnectionHandler

    @Inject
    lateinit var mediaServiceHandler: MediaServiceHandler

    override fun updateNotificationLayout() {
        Log.i("MediaplayerService", "Updating notification layout")
        mediaSession.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(getString(if (mediaSession.player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (mediaSession.player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            when (mediaSession.player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    )
                    .setIconResId(
                        when (mediaSession.player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        }
                    )
                    .setSessionCommand(CommandToggleRepeatMode)
                    .build()
            )
        )
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectionHandler.connect(mediaServiceHandler)
        notificationManager.startNotificationService(
            mediaSessionService = this,
            mediaSession = mediaSession
        )
        mediaServiceHandler.setMediaSessionInterface(this)
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