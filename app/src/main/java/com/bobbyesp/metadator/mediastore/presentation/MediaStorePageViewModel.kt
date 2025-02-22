package com.bobbyesp.metadator.mediastore.presentation

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import com.bobbyesp.mediaplayer.domain.repository.MusicScanner
import com.bobbyesp.utilities.mediastore.model.Song
import com.bobbyesp.utilities.states.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaStorePageViewModel(musicScanner: MusicScanner) : ViewModel() {
  private val _songs: MutableStateFlow<ResourceState<List<Song>>> =
      MutableStateFlow(ResourceState.Loading())
  val songs = _songs.asStateFlow()

  private val mediaStoreSongsFlow =
      musicScanner.observeMusicLibrary(null, null).map { musicTracks ->
        musicTracks.map { it.toSong() }
      }

  private fun songsCollection() {
    viewModelScope.launch(Dispatchers.IO) {
      mediaStoreSongsFlow.collectLatest { songs -> _songs.update { ResourceState.Success(songs) } }
    }
  }

  private fun reloadMediaStore() {
    _songs.update { ResourceState.Loading() }
    songsCollection()
  }

  fun onEvent(event: Events) {
    when (event) {
      is Events.StartObservingMediaStore -> songsCollection()

      is Events.ReloadMediaStore -> reloadMediaStore()
    }
  }

  companion object {
    fun MusicTrack.toSong(): Song {
      return Song(
          id = id,
          title = title,
          artist = artist ?: "",
          album = album ?: "",
          artworkPath = (artworkUri ?: "").toUri(),
          duration = duration?.toDouble() ?: 0.0,
          path = path,
          fileName = this.path.substringAfterLast("/"),
      )
    }

    interface Events {
      data object StartObservingMediaStore : Events

      data object ReloadMediaStore : Events
    }
  }
}
