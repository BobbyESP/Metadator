/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.player.media3

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController
import com.bobbyesp.metadator.player.api.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app's side of [PlaybackService]: a [MediaController] connected on first use. Commands issued
 * before the connection is ready are queued, so the first "play" is never lost.
 *
 * Must be called on the main thread, as every Media3 controller.
 */
class Media3PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
) : PlayerController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayDeque<(MediaController) -> Unit>()
    private var queue: List<Track> = emptyList()
    private var ticker: Job? = null

    private val listener =
        object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish()

            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(error = error.message)
            }
        }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let {
            action(it)
            return
        }
        pending.addLast(action)
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                connecting = false
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(listener)
                while (pending.isNotEmpty()) pending.removeFirst()(connected)
                publish()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        queue = tracks
        withController { player ->
            player.setMediaItems(
                tracks.map { it.toMediaItem() },
                startIndex.coerceIn(tracks.indices),
                0L,
            )
            player.shuffleModeEnabled = shuffle
            player.prepare()
            player.play()
        }
    }

    override fun togglePlayPause() = withController { player ->
        if (player.isPlaying) player.pause()
        else {
            if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition(0)
            player.play()
        }
    }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    override fun skipToNext() = withController { it.seekToNext() }

    override fun skipToPrevious() = withController { it.seekToPrevious() }

    override fun skipToQueueItem(index: Int) = withController { it.seekToDefaultPosition(index) }

    override fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun setRepeatMode(mode: RepeatMode) = withController {
        it.repeatMode =
            when (mode) {
                RepeatMode.Off -> Player.REPEAT_MODE_OFF
                RepeatMode.All -> Player.REPEAT_MODE_ALL
                RepeatMode.One -> Player.REPEAT_MODE_ONE
            }
    }

    override fun stop() = withController { player ->
        player.stop()
        player.clearMediaItems()
        queue = emptyList()
    }

    private fun publish() {
        val player = controller ?: return
        val index = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
        val current = queue.getOrNull(index)
        _state.value =
            PlaybackState(
                queue = if (player.mediaItemCount == 0) emptyList() else queue,
                currentIndex = index,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L,
                shuffle = player.shuffleModeEnabled,
                repeatMode =
                    when (player.repeatMode) {
                        Player.REPEAT_MODE_ONE -> RepeatMode.One
                        Player.REPEAT_MODE_ALL -> RepeatMode.All
                        else -> RepeatMode.Off
                    },
                error = _state.value.error.takeIf { player.playerError != null },
            )
        updateTicker(player.isPlaying)
    }

    /** The position moves on its own while playing; nothing reports it, so it is polled. */
    private fun updateTicker(playing: Boolean) {
        if (playing && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    delay(TICK_MS)
                    controller?.let { player ->
                        _state.value =
                            _state.value.copy(positionMs = player.currentPosition.coerceAtLeast(0))
                    }
                }
            }
        } else if (!playing) {
            ticker?.cancel()
            ticker = null
        }
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.value.toString())
            .setUri(Uri.parse(ref.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setAlbumArtist(albumArtist)
                    .setArtworkUri(artworkRef?.let { Uri.parse(it.uri) })
                    .build()
            )
            .build()

    private companion object {
        const val TICK_MS = 500L
    }
}
