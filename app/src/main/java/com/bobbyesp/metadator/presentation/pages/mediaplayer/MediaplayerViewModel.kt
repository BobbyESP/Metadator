package com.bobbyesp.metadator.presentation.pages.mediaplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaServiceHandler
import com.bobbyesp.mediaplayer.service.MediaState
import com.bobbyesp.mediaplayer.service.PlayerEvent
import com.bobbyesp.mediaplayer.service.queue.SongsQueue
import com.bobbyesp.model.Song
import com.bobbyesp.utilities.Time.formatDuration
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver.Advanced.getSongs
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver.Advanced.observeSongs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class MediaplayerViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val serviceHandler: MediaServiceHandler,
    private val mediaSession: MediaSession,
    val connectionHandler: ConnectionHandler
) : ViewModel() {
    private val mutableMediaplayerPageState = MutableStateFlow(MediaplayerPageState())
    val pageViewState = mutableMediaplayerPageState.asStateFlow()

    val songsFlow = applicationContext.contentResolver.observeSongs()

    val isPlaying = serviceHandler.isThePlayerPlaying

    val playingSong = serviceHandler.currentMediaItem.asStateFlow()

    data class MediaplayerPageState(
        val uiState: PlayerState = PlayerState.Initial,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            serviceHandler.mediaState.collectLatest { mediaState ->
                when (mediaState) {
                    is MediaState.Buffering -> calculateProgressValues(mediaState.progress)
                    is MediaState.Playing -> mutableMediaplayerPageState.update {
                        (it.uiState as? PlayerState.Ready)?.let { readyState ->
                            it.copy(
                                uiState = readyState.copy(isPlaying = mediaState.isPlaying)
                            )
                        } ?: it
                    }

                    is MediaState.Idle -> mutableMediaplayerPageState.update { it.copy(uiState = PlayerState.Initial) }
                    is MediaState.Progress -> calculateProgressValues(mediaState.progress)
                    is MediaState.Ready -> {
                        mutableMediaplayerPageState.update {
                            it.copy(
                                uiState = PlayerState.Ready(duration = mediaState.duration)
                            )
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            serviceHandler.onPlayerEvent(PlayerEvent.PlayPause)
        }
    }

    fun playShuffledQueue(firstSong: Song) {
        playRandomQueue(firstSong)
        viewModelScope.launch {
            serviceHandler.onPlayerEvent(PlayerEvent.PlayPause)
        }
    }

    fun seekTo(progress: Float) {
        val duration = (pageViewState.value.uiState as? PlayerState.Ready)?.duration ?: 0L
        val seekPosition = (progress * duration).toLong()
        viewModelScope.launch {
            serviceHandler.seekTo(seekPosition)
        }
    }


    private fun playRandomQueue(firstSong: Song) {
        viewModelScope.launch {
            val copiedList = applicationContext.contentResolver.getSongs().toMutableList()

            copiedList.shuffle()

            // Move the firstSong to the front of the list
            copiedList.remove(firstSong)
            copiedList.add(0, firstSong)

            Log.d("MediaplayerViewModel", "playRandomQueue: copiedList: $copiedList")

            loadQueueSongs(copiedList)
        }
    }

    private fun loadSongInfo(song: Song) {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(song.path)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.artworkPath)
                    .build()
            ).build()

        viewModelScope.launch {
            serviceHandler.setMediaItem(mediaItem)
        }
    }

    private fun loadQueueSongs(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(Uri.fromFile(File(song.path)))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.artworkPath)
                        .build()
                ).build()
        }

        viewModelScope.launch {
            val queue = SongsQueue(title = null, items = mediaItems)
            serviceHandler.playQueue(queue)
        }
    }

    private fun calculateProgressValues(currentProgress: Long) {
        if (currentProgress <= 0) {
            mutableMediaplayerPageState.update {
                (it.uiState as? PlayerState.Ready)?.let { readyState ->
                    it.copy(
                        uiState = readyState.copy(
                            progress = 0f,
                            progressString = "00:00",
                            isPlaying = false
                        )
                    )
                } ?: it
            }
            return
        }

        (pageViewState.value.uiState as? PlayerState.Ready)?.let { readyState ->
            val progress = currentProgress.toFloat() / readyState.duration
            val progressString = formatDuration(currentProgress)
            mutableMediaplayerPageState.update {
                it.copy(
                    uiState = readyState.copy(
                        progress = progress,
                        progressString = progressString
                    )
                )
            }
        } ?: calculateProgressValues(0L)
    }

    override fun onCleared() {
        viewModelScope.launch {
            serviceHandler.killPlayer()
        }
        super.onCleared()
    }

    sealed interface PlayerState {
        data object Initial : PlayerState
        data class Ready(
            val progress: Float = 0f,
            val progressString: String = "00:00",
            val duration: Long = 0L,
            val isPlaying: Boolean = false
        ) : PlayerState
    }
}