/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Plays in the background, with the system's media notification and lock-screen controls. Media3
 * builds the notification; there is nothing custom to maintain.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
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

        val builder = MediaSession.Builder(this, player)
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