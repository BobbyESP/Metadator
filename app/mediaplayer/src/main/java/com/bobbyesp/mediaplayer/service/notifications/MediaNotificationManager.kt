package com.bobbyesp.mediaplayer.service.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.bobbyesp.mediaplayer.R
import org.koin.core.component.KoinComponent

@UnstableApi
class MediaNotificationManager @OptIn(UnstableApi::class) constructor(
    private val context: Context,
    private val player: ExoPlayer
) : KoinComponent {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        ensureNotificationChannel(notificationManagerCompat)
    }

    val notificationListener = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(
            notificationId: Int,
            dismissedByUser: Boolean
        ) {
            Log.d(
                "MediaNotificationManager",
                "onNotificationCancelled: notification cancelled"
            )
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            Log.d(
                "MediaNotificationManager",
                "onNotificationPosted: notification posted"
            )
        }
    }

    @UnstableApi
    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession
    ) {
        buildNotification(mediaSession)
        startForegroundNotification(mediaSessionService)
    }

    @UnstableApi
    private fun buildNotification(mediaSession: MediaSession) {
        PlayerNotificationManager.Builder(context, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
            .setMediaDescriptionAdapter(
                MediaNotificationAdapter(
                    context = context,
                    pendingIntent = mediaSession.sessionActivity
                )
            )
            .setCustomActionReceiver(MediaCustomActionReceiver())
            .setSmallIconResourceId(R.drawable.metadator_logo_player)
            .setNotificationListener(notificationListener)
            .build()
            .also {
                with(it) {
                    setMediaSessionToken(mediaSession.sessionCompatToken)
                    setPriority(NotificationCompat.PRIORITY_LOW)
                    setPlayer(mediaSession.player)
                }
            }
    }

    private fun startForegroundNotification(mediaSessionService: MediaSessionService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(
                "MediaNotificationManager",
                "startForegroundNotification: creating notification for API >= 26"
            )
            val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()

            mediaSessionService.startForeground(NOTIFICATION_ID, notification)
        } else {
            Log.d(
                "MediaNotificationManager",
                "startForegroundNotification: creating notification for API < 26"
            )
            val notification = NotificationCompat.Builder(context)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            mediaSessionService.startForeground(NOTIFICATION_ID, notification)
        }
    }

    @OptIn(UnstableApi::class)
    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (Util.SDK_INT < 26 || notificationManagerCompat.getNotificationChannel(
                NOTIFICATION_CHANNEL_ID
            ) != null
        ) {
            return
        }

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManagerCompat.createNotificationChannel(channel)
    }


    companion object {
        private const val NOTIFICATION_ID = 200
        private const val NOTIFICATION_CHANNEL_NAME = "notification_channel_1"
        private const val NOTIFICATION_CHANNEL_ID = "notification_channel_id_1"
    }
}