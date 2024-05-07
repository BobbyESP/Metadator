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
import com.google.common.util.concurrent.MoreExecutors
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

    /**
     * This method is called by the system every time a client explicitly starts the service by calling
     * [android.content.Context.startService], providing the arguments it supplied and a unique integer
     * token representing the start request.
     */
    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Connects the media service handler to the connection handler
        connectionHandler.connect(mediaServiceHandler)

        // Starts the notification service with the current media session service and media session
        notificationManager.startNotificationService(
            mediaSessionService = this, mediaSession = mediaSession
        )

        // Sets the media session interface for the media service handler
        mediaServiceHandler.setMediaSessionInterface(mediaSessionLayoutHandler)

        // Builds a media controller asynchronously with the current media session token
        val controllerFuture = MediaController.Builder(this, mediaSession.token).buildAsync()

        // Adds a listener to the controller future that gets the result of the future when it's ready
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        // Calls the super implementation of onStartCommand
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        connectionHandler.disconnect()
        mediaSession.run {
            release()
            clearListener()
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
        if (!mediaSession.player.playWhenReady || mediaSession.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

    inner class MusicBinder : Binder() {
        val service: MediaplayerService
            get() = this@MediaplayerService
    }
}