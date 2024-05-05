package com.bobbyesp.mediaplayer.service.notifications.customLayout

import android.content.Context
import androidx.core.content.ContextCompat.getString
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import com.bobbyesp.mediaplayer.R
import com.bobbyesp.mediaplayer.service.MediaLibrarySessionCallback
import com.bobbyesp.mediaplayer.service.MediaServiceHandler.Companion.CommandToggleRepeatMode
import com.bobbyesp.mediaplayer.service.MediaServiceHandler.Companion.CommandToggleShuffle
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaSessionLayoutHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSession: MediaLibraryService.MediaLibrarySession,
    private val mediaSessionCallback: MediaLibrarySessionCallback
) : MediaSessionLayoutHandler {

    val commandButtons = ImmutableList.of<CommandButton>(
        CommandButton.Builder()
            .setDisplayName(
                getString(
                    context,
                    if (mediaSession.player.shuffleModeEnabled) R.string.action_shuffle_on else R.string.action_shuffle_off
                )
            )
            .setIconResId(if (mediaSession.player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
            .setSessionCommand(CommandToggleShuffle).build(),
        CommandButton.Builder()
            .setDisplayName(
                getString(
                    context,
                    when (mediaSession.player.repeatMode) {
                        REPEAT_MODE_OFF -> R.string.repeat_mode_off
                        REPEAT_MODE_ONE -> R.string.repeat_mode_one
                        REPEAT_MODE_ALL -> R.string.repeat_mode_all
                        else -> throw IllegalStateException()
                    }
                )
            ).setIconResId(
                when (mediaSession.player.repeatMode) {
                    REPEAT_MODE_OFF -> R.drawable.repeat
                    REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                    REPEAT_MODE_ALL -> R.drawable.repeat_on
                    else -> throw IllegalStateException()
                }
            ).setSessionCommand(CommandToggleRepeatMode).build()
    )

    override fun updateNotificationLayout() {
        mediaSession.setCustomLayout(commandButtons)
    }
}