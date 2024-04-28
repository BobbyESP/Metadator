package com.bobbyesp.mediaplayer.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import com.bobbyesp.mediaplayer.ext.toMediaItem
import com.bobbyesp.mediaplayer.service.queue.EmptyQueue
import com.bobbyesp.mediaplayer.service.queue.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaItem = MutableStateFlow<MediaItem?>(null)

    val isThePlayerPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
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

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        currentQueue = queue
        queueTitle = null
        if (queue.preloadItem != null) {
            setMediaItem(queue.preloadItem!!.toMediaItem())
            player.playWhenReady = playWhenReady
        }

        // Launch a new coroutine in the main thread
        scope.launch {
            // Get the initial state of the queue in the IO thread
            // This is a suspending operation and will not block the main thread
            val initialState = withContext(Dispatchers.IO) { queue.getInitialData() }

            // Set the title of the queue
            queueTitle = initialState.title

            // Check if the initial state has any items and if the player is not idle or the preload item is null
            // If both conditions are met, proceed with the rest of the code
            if (initialState.items.isNotEmpty() && !(queue.preloadItem != null && player.playbackState == STATE_IDLE)) {
                // If the preload item is not null, add media items to the player
                if (queue.preloadItem != null) {
                    // Add media items from the start of the list to the current media item index
                    player.addMediaItems(
                        0,
                        initialState.items.subList(0, initialState.mediaItemIndex)
                    )
                    // Add media items from the current media item index to the end of the list
                    player.addMediaItems(
                        initialState.items.subList(
                            initialState.mediaItemIndex + 1,
                            initialState.items.size
                        )
                    )
                } else {
                    // If the preload item is null, set media items to the player
                    // If the media item index is greater than 0, use it as the start position
                    // Otherwise, use 0 as the start position
                    player.setMediaItems(
                        initialState.items,
                        if (initialState.mediaItemIndex > 0) initialState.mediaItemIndex else 0,
                        initialState.position
                    )
                    // Prepare the player for playback
                    player.prepare()
                    // Set the player to start playback when it's ready
                    player.playWhenReady = playWhenReady
                }
            }
        }
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

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaItem.value = player.currentMediaItem
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

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        if (reason == STATE_IDLE) {
            currentQueue = EmptyQueue
            queueTitle = null
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