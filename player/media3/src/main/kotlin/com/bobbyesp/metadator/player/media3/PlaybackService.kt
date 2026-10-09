/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.Ac4Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Plays in the background, with the system's media notification and lock-screen controls. Media3
 * builds the notification; the service gives it the app's icon and two buttons, shuffle and repeat.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    private val toggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
    private val cycleRepeat = SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY)

    private val buttonsListener =
        object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = showButtons()

            override fun onRepeatModeChanged(repeatMode: Int) = showButtons()
        }

    private val callback =
        object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult =
                MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                            .add(toggleShuffle)
                            .add(cycleRepeat)
                            .build()
                    )
                    .build()

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                val player = session.player
                when (customCommand.customAction) {
                    ACTION_TOGGLE_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                    ACTION_CYCLE_REPEAT ->
                        player.repeatMode =
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                    else ->
                        return Futures.immediateFuture(
                            SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                        )
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_metadator_notification)
            }
        )
        val player =
            audioPlayerBuilder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
        QueueShuffler(player)
        player.addListener(buttonsListener)

        val builder =
            MediaSession.Builder(this, player)
                .setCallback(callback)
                .setMediaButtonPreferences(buttons(player))
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        }
        session = builder.build()
    }

    private fun showButtons() {
        session?.let { it.setMediaButtonPreferences(buttons(it.player)) }
    }

    /** Shuffle and repeat, beside the transport controls the notification already has. */
    private fun buttons(player: Player): List<CommandButton> =
        listOf(
            CommandButton.Builder(
                    if (player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON
                    else CommandButton.ICON_SHUFFLE_OFF
                )
                .setDisplayName(getString(R.string.notification_shuffle))
                .setSessionCommand(toggleShuffle)
                .build(),
            when (player.repeatMode) {
                    Player.REPEAT_MODE_ALL ->
                        CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                            .setDisplayName(getString(R.string.notification_repeat_all))
                    Player.REPEAT_MODE_ONE ->
                        CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
                            .setDisplayName(getString(R.string.notification_repeat_one))
                    else ->
                        CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
                            .setDisplayName(getString(R.string.notification_repeat_off))
                }
                .setSessionCommand(cycleRepeat)
                .build(),
        )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away stops playback that is paused; music that plays keeps playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private companion object {
        const val ACTION_TOGGLE_SHUFFLE = "com.bobbyesp.metadator.player.TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "com.bobbyesp.metadator.player.CYCLE_REPEAT"
    }
}

/**
 * ExoPlayer's default factories reach every renderer and container it has, so R8 keeps its video,
 * subtitle and image playback. Naming the audio ones lets it drop the rest.
 */
@OptIn(UnstableApi::class)
private fun audioPlayerBuilder(context: Context): ExoPlayer.Builder {
    val renderers = RenderersFactory { handler, _, audioListener, _, _ ->
        arrayOf(
            MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioListener)
        )
    }
    // Content URIs carry no extension, so a file is sniffed in this order: the one
    // DefaultExtractorsFactory uses, strictest first.
    val extractors = ExtractorsFactory {
        val noSubtitles = SubtitleParser.Factory.UNSUPPORTED
        arrayOf(
            FlacExtractor(),
            WavExtractor(),
            FragmentedMp4Extractor(noSubtitles),
            Mp4Extractor(noSubtitles),
            AmrExtractor(),
            OggExtractor(),
            MatroskaExtractor(noSubtitles),
            AdtsExtractor(),
            Ac3Extractor(),
            Ac4Extractor(),
            Mp3Extractor(),
        )
    }
    return ExoPlayer.Builder(
        context,
        renderers,
        ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context), extractors),
    )
}
