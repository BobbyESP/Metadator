package com.bobbyesp.mediaplayer.service.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager.CustomActionReceiver
import com.bobbyesp.mediaplayer.R

@UnstableApi
class MediaCustomActionReceiver : CustomActionReceiver {
    companion object {
        const val CUSTOM_ACTION_SHUFFLE =
            "com.bobbyesp.mediaplayer.service.notifications.ACTION_SHUFFLE"
        const val CUSTOM_ACTION_REPEAT =
            "com.bobbyesp.mediaplayer.service.notifications.ACTION_REPEAT"
    }

    override fun createCustomActions(
        context: Context,
        instanceId: Int
    ): MutableMap<String, NotificationCompat.Action> {
        val shuffleAction = NotificationCompat.Action.Builder(
            R.drawable.shuffle,
            context.getString(R.string.action_shuffle_on),
            PendingIntent.getBroadcast(
                context,
                instanceId,
                Intent(CUSTOM_ACTION_SHUFFLE).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        val repeatAction = NotificationCompat.Action.Builder(
            R.drawable.repeat,
            context.getString(R.string.repeat_mode_all),
            PendingIntent.getBroadcast(
                context,
                instanceId,
                Intent(CUSTOM_ACTION_REPEAT).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        return mutableMapOf(
            CUSTOM_ACTION_SHUFFLE to shuffleAction,
            CUSTOM_ACTION_REPEAT to repeatAction
        )
    }

    override fun getCustomActions(player: Player): MutableList<String> {
        return mutableListOf(CUSTOM_ACTION_SHUFFLE, CUSTOM_ACTION_REPEAT)
    }

    override fun onCustomAction(player: Player, action: String, intent: Intent) {
        when (action) {
            CUSTOM_ACTION_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
            CUSTOM_ACTION_REPEAT -> {
                when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> player.repeatMode = Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> player.repeatMode = Player.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ONE -> player.repeatMode = Player.REPEAT_MODE_OFF
                }
            }
        }
    }
}