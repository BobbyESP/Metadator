package com.bobbyesp.mediaplayer.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * This class is responsible for handling media playback events and managing the state of the media player.
 * It provides methods to control the media player such as play, pause, stop, and seek.
 * It also provides methods to manage the media queue such as set, add, and remove media items.
 */
@UnstableApi
class MediaServiceHandler @Inject constructor(
    private val player: ExoPlayer
) : Player.Listener, PlaybackStatsListener.Callback {
    private val _mediaState = MutableStateFlow<MediaState>(MediaState.Idle)
    val mediaState = _mediaState.asStateFlow()

    private val _playerActions = MutableStateFlow<PlayerEvent?>(null)
    val playerActions = _playerActions.asStateFlow()

    val isThePlayerPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var job: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _mediaState.update {
            MediaState.Playing(isPlaying)
        }
        isThePlayerPlaying.update {
            isPlaying
        }
        super.onIsPlayingChanged(isPlaying)
    }

    init {
        player.addListener(this)
        job = Job()
    }

    /**
     * Stops the player, clears the media queue, and releases resources.
     */
    fun killPlayer() {
        player.stop()
        player.clearMediaItems()
        player.release()
    }

    /**
     * Sets a single media item as the current item and prepares the player for playback.
     * @param mediaItem The media item to be set.
     */
    fun setMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    /**
     * Sets a list of media items as the current queue and prepares the player for playback.
     * @param mediaItems The list of media items to be set.
     */
    fun setMediaItems(mediaItems: List<MediaItem>) {
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    /**
     * Adds a media item to the end of the queue and prepares the player for playback.
     * @param mediaItem The media item to be added.
     */
    fun addMediaItem(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        player.prepare()
    }

    /**
     * Removes a media item from the queue.
     * @param index The media item to be removed.
     */
    fun removeMediaItemAtIndex(index: Int) {
        player.removeMediaItem(index)
    }

    /**
     * Moves a media item within the queue.
     * @param currentIndex The current index of the media item.
     * @param newIndex The new index for the media item.
     */
    fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        player.moveMediaItem(currentIndex, newIndex)
    }

    /**
     * Skips to the next media item in the queue.
     */
    fun skipToNext() {
        player.seekToNext()
    }

    /**
     * Skips to the previous media item in the queue.
     */
    fun skipToPrevious() {
        player.seekToPrevious()
    }

    /**
     * Seeks to a specific position in the current media item.
     * @param positionMs The position to seek to, in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    startProgressUpdate()
                }
            }

            is PlayerEvent.Stop -> {
                /**THIS KILLS THE PLAYER**/
                player.stop()
                stopProgressUpdate()
            }

            is PlayerEvent.Next -> {
                player.seekToNext()
            }

            is PlayerEvent.Previous -> {
                player.seekToPrevious()
            }

            is PlayerEvent.UpdateProgress -> {
                player.seekTo(playerEvent.updatedProgress)
            }
        }
    }

    fun getActualMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    fun getActualMediaItemIndex(): Int {
        return player.currentMediaItemIndex
    }

    fun getActualMediaItemMetadata(): MediaMetadata? {
        return player.currentMediaItem?.mediaMetadata
    }

    fun getActualMediaItemDuration(): Long {
        return player.duration
    }

    fun getActualMediaItemPosition(): Long {
        return player.currentPosition
    }

    fun getActualMediaItemPositionPercentage(): Float {
        return player.currentPosition / player.duration.toFloat()
    }


    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _mediaState.update {
                MediaState.Buffering(player.duration)
            }

            ExoPlayer.STATE_READY -> _mediaState.update {
                MediaState.Ready(player.duration)
            }

            ExoPlayer.STATE_ENDED -> _mediaState.update {
                MediaState.Idle
            }

            ExoPlayer.STATE_IDLE -> _mediaState.update {
                MediaState.Idle
            }
        }
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(250)
            _mediaState.update {
                MediaState.Progress(player.currentPosition)
            }
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _mediaState.update {
            MediaState.Playing(false)
        }
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        TODO("Not yet implemented")
    }
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object Stop : PlayerEvent()
    data object Next : PlayerEvent()
    data object Previous : PlayerEvent()
    data class UpdateProgress(val updatedProgress: Long) : PlayerEvent()
}

sealed class MediaState { //TODO: NOT USE SEALED CLASSES
    data object Idle : MediaState()
    data class Ready(val duration: Long) : MediaState()
    data class Progress(val progress: Long) : MediaState()
    data class Buffering(val progress: Long) : MediaState()
    data class Playing(val isPlaying: Boolean) : MediaState()
}