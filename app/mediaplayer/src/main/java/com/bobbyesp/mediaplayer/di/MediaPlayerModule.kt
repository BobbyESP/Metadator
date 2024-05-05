package com.bobbyesp.mediaplayer.di

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaLibrarySessionCallback
import com.bobbyesp.mediaplayer.service.MediaServiceHandler
import com.bobbyesp.mediaplayer.service.notifications.MediaNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object MediaPlayerModule {
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).setUsage(C.USAGE_MEDIA)
            .build()

    @Provides
    @Singleton
    @UnstableApi
    fun providePlayer(
        @ApplicationContext context: Context, audioAttributes: AudioAttributes
    ): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setTrackSelector(DefaultTrackSelector(context))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true
        )
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context, player: ExoPlayer
    ): MediaNotificationManager = MediaNotificationManager(
        context = context, player = player
    )

    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer,
        mediaLibrarySessionCallback: MediaLibrarySessionCallback,
    ): MediaSession =
        MediaLibrarySession.Builder(context, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    @Provides
    @Singleton
    fun provideServiceHandler(
        player: ExoPlayer
    ): MediaServiceHandler =
        MediaServiceHandler(
            player = player
        )

    @Provides
    @Singleton
    fun provideConnectionHandler(): ConnectionHandler = ConnectionHandler()
}