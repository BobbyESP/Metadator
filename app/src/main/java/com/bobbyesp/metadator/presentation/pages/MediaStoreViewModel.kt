package com.bobbyesp.metadator.presentation.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.model.Song
import com.bobbyesp.utilities.mediastore.MediaStoreFilterType
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MediaStorePageViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context
) : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadMediaStoreTracks(applicationContext)
        }
    }

    private val mutablePageViewState = MutableStateFlow(PageViewState())
    val pageViewState = mutablePageViewState.asStateFlow()

    data class PageViewState(
        val state: MediaStorePageState = MediaStorePageState.Loading,
        val filter: MediaStoreFilterType? = null,
        val songs: List<Song> = emptyList()
    )

    /**
     * Loads all songs from the media store db
     * @return a list of songs
     */
    suspend fun loadMediaStoreTracks(
        context: Context
    ) {
        updateState(MediaStorePageState.Loading)

        val songs = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            async {
                MediaStoreReceiver.getAllSongsFromMediaStore(
                    applicationContext = context,
                )
            }.await()
        }

        mutablePageViewState.update {
            it.copy(songs = songs)
        }

        updateState(MediaStorePageState.Loaded)
    }

    /**
     * Loads all songs from the media store db
     * @return a list of songs
     */
    suspend fun silentMediaStoreTracksLoad(
        context: Context,
        onFinish: suspend () -> Unit
    ) {
        val songs = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            async {
                MediaStoreReceiver.getAllSongsFromMediaStore(
                    applicationContext = context,
                )
            }.await()
        }

        mutablePageViewState.update {
            it.copy(songs = songs)
        }
        onFinish()
    }

    /**
     * Loads all songs from the media store db
     * @return a list of songs
     */
    suspend fun loadMediaStoreWithFilter(
        context: Context,
        filter: String,
        filterType: MediaStoreFilterType? = pageViewState.value.filter
    ) {
        updateState(MediaStorePageState.Loading)

        val songs = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            async {
                MediaStoreReceiver.getAllSongsFromMediaStore(
                    applicationContext = context, searchTerm = filter, filterType = filterType
                )
            }.await()
        }

        mutablePageViewState.update {
            it.copy(songs = songs)
        }

        updateState(MediaStorePageState.Loaded)
    }

    /**
     * Updates the state of the page
     * @param state the new state
     */
    private fun updateState(state: MediaStorePageState) {
        viewModelScope.launch(Dispatchers.Main) {
            mutablePageViewState.update {
                it.copy(state = state)
            }
        }
    }

    fun updateFilter(filter: MediaStoreFilterType?) {
        if (filter == pageViewState.value.filter) {
            mutablePageViewState.update {
                it.copy(filter = null)
            }
        } else {
            mutablePageViewState.update {
                it.copy(filter = filter)
            }
        }
    }
}

/**
 * The state of the page
 */
sealed class MediaStorePageState {
    data object Loading : MediaStorePageState()
    data object Loaded : MediaStorePageState()
    data object Error : MediaStorePageState()
}