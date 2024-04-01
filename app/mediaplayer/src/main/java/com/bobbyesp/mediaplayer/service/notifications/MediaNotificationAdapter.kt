package com.bobbyesp.mediaplayer.service.notifications

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import coil.ImageLoader

@UnstableApi
class MediaNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter {
    override fun getCurrentContentTitle(player: Player): CharSequence {
        return player.mediaMetadata.albumTitle
            ?: context.getString(androidx.media3.ui.R.string.exo_track_unknown)
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        return pendingIntent
    }

    override fun getCurrentContentText(player: Player): CharSequence {
        return player.mediaMetadata.displayTitle
            ?: context.getString(androidx.media3.ui.R.string.exo_track_unknown)
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        val loader = ImageLoader(context)
        val request = coil.request.ImageRequest.Builder(context)
            .data(player.mediaMetadata.artworkData)
            .target { drawable ->
                callback.onBitmap(drawable.toBitmap())
            }
            .build()
        loader.enqueue(request)

        return null
    }

}