package com.bobbyesp.mediaplayer.service

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.SessionCommand
import com.bobbyesp.mediaplayer.ext.toMediaItem
import com.bobbyesp.mediaplayer.service.notifications.customLayout.MediaSessionLayoutHandler
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

    val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)
    val canSkipNext: Boolean
        get() = player.hasNextMediaItem()

    val canSkipPrevious: Boolean = false

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var mediaSessionInterface: MediaSessionLayoutHandler

    fun setMediaSessionInterface(mediaSessionInterface: MediaSessionLayoutHandler) {
        this.mediaSessionInterface = mediaSessionInterface
    }

    init {
        player.addListener(this)
        repeatMode.update { player.repeatMode }
        shuffleModeEnabled.update { player.shuffleModeEnabled }
        job = Job()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        mediaSessionInterface.updateNotificationLayout()
        _mediaState.update {
            MediaState.Playing(isPlaying)
        }
        this.isPlaying.update {
            isPlaying
        }
        super.onIsPlayingChanged(isPlaying)
    }

    /**
     * Stops the player, clears the media queue, and releases resources.
     */
    fun killPlayer() {
        player.removeListener(this)
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

            PlayerEvent.ToggleRepeat -> {
                val repeatMode = when (player.repeatMode) {
                    REPEAT_MODE_OFF -> REPEAT_MODE_ONE
                    REPEAT_MODE_ONE -> REPEAT_MODE_ALL
                    REPEAT_MODE_ALL -> REPEAT_MODE_OFF
                    else -> REPEAT_MODE_OFF
                }
                player.repeatMode = repeatMode
            }

            PlayerEvent.ToggleShuffle -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaItem.value = player.currentMediaItem
        }
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

    /**
     * This method is triggered when the shuffle mode is enabled or disabled in the player.
     * It updates the notification layout and, if shuffle mode is enabled, it shuffles the media items in the player.
     *
     * @param shuffleModeEnabled A boolean indicating whether shuffle mode is enabled.
     */
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        // Update the notification layout
        mediaSessionInterface.updateNotificationLayout()
        this.shuffleModeEnabled.update { shuffleModeEnabled }

        // If shuffle mode is enabled
        if (shuffleModeEnabled) {
            // Always put current playing item at first
            // Create an array of indices representing the media items in the player
            val shuffledIndices = IntArray(player.mediaItemCount) { it }

            // Shuffle the indices
            shuffledIndices.shuffle()

            // Swap the current media item index with the first index
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex

            // Set the shuffle order in the player using the shuffled indices
            player.setShuffleOrder(
                ShuffleOrder.DefaultShuffleOrder(
                    shuffledIndices,
                    System.currentTimeMillis()
                )
            )
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        mediaSessionInterface.updateNotificationLayout()
        this.repeatMode.update { repeatMode }
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

    companion object {
        const val ACTION_TOGGLE_LIBRARY = "TOGGLE_LIBRARY"
        const val ACTION_TOGGLE_LIKE = "TOGGLE_LIKE"
        const val ACTION_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT_MODE = "TOGGLE_REPEAT_MODE"
        val CommandToggleLibrary = SessionCommand(ACTION_TOGGLE_LIBRARY, Bundle())
        val CommandToggleLike = SessionCommand(ACTION_TOGGLE_LIKE, Bundle())
        val CommandToggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle())
        val CommandToggleRepeatMode = SessionCommand(ACTION_TOGGLE_REPEAT_MODE, Bundle())
    }
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object Stop : PlayerEvent()
    data object Next : PlayerEvent()
    data object Previous : PlayerEvent()
    data object ToggleShuffle : PlayerEvent()
    data object ToggleRepeat : PlayerEvent()
    data class UpdateProgress(val updatedProgress: Long) : PlayerEvent()
}

sealed class MediaState { //TODO: NOT USE SEALED CLASSES
    data object Idle : MediaState()
    data class Ready(val duration: Long) : MediaState()
    data class Progress(val progress: Long) : MediaState()
    data class Buffering(val progress: Long) : MediaState()
    data class Playing(val isPlaying: Boolean) : MediaState()
}