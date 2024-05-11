package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.pagination.TracksPagingSource
import com.bobbyesp.metadator.features.spotify.domain.services.SpotifyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpMetadataBottomSheetContentViewModel @Inject constructor(
    private val spotifyService: SpotifyService
) : ViewModel() {
    private lateinit var spotifyApi: SpotifyAppApi

    init {
        viewModelScope.launch {
            spotifyApi = spotifyService.getSpotifyApi()
        }
    }

    private var searchJob: Job? = null

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    data class ViewState(
        val viewState: ViewSearchState = ViewSearchState.Idle,
        val searchedTracks: Flow<PagingData<Track>> = emptyFlow(),
    )

    private fun getTracksPaginatedData(query: String) {
        val tracksPager = Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 40,
            ),
            pagingSourceFactory = {
                TracksPagingSource(
                    spotifyApi = spotifyApi,
                    query = query,
                )
            }
        ).flow.cachedIn(viewModelScope)

        mutableViewStateFlow.update {
            it.copy(
                searchedTracks = tracksPager
            )
        }

    }

    companion object {
        sealed class ViewSearchState {
            data object Idle : ViewSearchState() // Initial state - May be deleted
            data object Loading : ViewSearchState()
            data object Success : ViewSearchState()
            data class Error(val error: String) : ViewSearchState()
        }
    }
}