package com.bobbyesp.metadator.presentation.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver.Advanced.observeSongs
import com.bobbyesp.utilities.mediastore.model.Song
import com.bobbyesp.utilities.states.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaStorePageViewModel(
    context: Context
) : ViewModel() {
    private val _songs: MutableStateFlow<ResourceState<List<Song>>> =
        MutableStateFlow(ResourceState.Loading())
    val songs = _songs.asStateFlow()

    private val mediaStoreSongsFlow =
        context.contentResolver.observeSongs()

    private fun songsCollection() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaStoreSongsFlow.collectLatest { songs ->
                _songs.update { ResourceState.Success(songs) }
            }
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
        interface Events {
            data object StartObservingMediaStore : Events
            data object ReloadMediaStore : Events
        }
    }
}