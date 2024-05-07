package com.bobbyesp.mediaplayer.service

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.bobbyesp.mediaplayer.service.MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE
import com.bobbyesp.mediaplayer.service.MediaSessionConstants.ACTION_TOGGLE_SHUFFLE
import com.bobbyesp.mediaplayer.service.MediaSessionConstants.CommandToggleRepeatMode
import com.bobbyesp.mediaplayer.service.MediaSessionConstants.CommandToggleShuffle
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaLibrarySessionCallback @Inject constructor(
    @ApplicationContext val context: Context,
) : MediaLibrarySession.Callback {

    private val availableCommands = listOf(
        CommandToggleShuffle,
        CommandToggleRepeatMode
    )

    override fun onConnect(
        session: MediaSession, //I've checked the MediaSession class and it is correct. Not the problem of the player
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()

        availableCommands.forEach {
            availableSessionCommands.add(it)
        }

        return MediaSession.ConnectionResult.accept(
            availableSessionCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled

            ACTION_TOGGLE_REPEAT_MODE -> session.player.repeatMode =
                when (session.player.repeatMode) {
                    REPEAT_MODE_OFF -> REPEAT_MODE_ONE
                    REPEAT_MODE_ONE -> REPEAT_MODE_ALL
                    REPEAT_MODE_ALL -> REPEAT_MODE_OFF
                    else -> REPEAT_MODE_OFF
                }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

}