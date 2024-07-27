package com.bobbyesp.metadator.presentation.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver.Advanced.observeSongs
import com.bobbyesp.utilities.model.Song
import com.bobbyesp.utilities.states.ResourceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaStorePageViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    private val _songs: MutableStateFlow<ResourceState<List<Song>>> =
        MutableStateFlow(ResourceState.Loading())
    val songs = _songs.asStateFlow()

    private val mediaStoreSongsFlow =
        applicationContext.contentResolver.observeSongs()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mediaStoreSongsFlow.collectLatest { songs ->
                _songs.update { ResourceState.Success(songs) }
            }
        }
    }

    fun reloadMediaStore() {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.update { ResourceState.Loading() }
            mediaStoreSongsFlow.collectLatest { songs ->
                _songs.update { ResourceState.Success(songs) }
            }
        }
    }
}