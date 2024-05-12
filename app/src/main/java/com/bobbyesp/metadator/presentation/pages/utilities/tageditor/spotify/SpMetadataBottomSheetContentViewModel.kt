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
import kotlinx.coroutines.Dispatchers
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
        val stage: BottomSheetStage = BottomSheetStage.SEARCH,
        val viewState: SearchStageViewState = SearchStageViewState.Idle,
        val searchedTracks: Flow<PagingData<Track>> = emptyFlow(),
        val selectedTrack: Track? = null
    )

    fun searchTracks(query: String) {
        searchJob?.cancel()
        updateViewState(SearchStageViewState.Loading)
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            getTracksPaginatedData(query)
        }
        updateViewState(SearchStageViewState.Success)
    }

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

    fun chooseTrack(track: Track) {
        mutableViewStateFlow.update {
            it.copy(
                selectedTrack = track
            )
        }
        updateStage(BottomSheetStage.TRACK_DETAILS)
    }

    fun updateStage(stage: BottomSheetStage) {
        mutableViewStateFlow.update {
            it.copy(
                stage = stage
            )
        }
    }

    private fun updateViewState(viewState: SearchStageViewState) {
        mutableViewStateFlow.update {
            it.copy(
                viewState = viewState
            )
        }
    }

    companion object {
        sealed class SearchStageViewState {
            data object Idle : SearchStageViewState() // Initial state - May be deleted
            data object Loading : SearchStageViewState()
            data object Success : SearchStageViewState()
            data class Error(val error: String) : SearchStageViewState()
        }


        enum class BottomSheetStage {
            SEARCH,
            TRACK_DETAILS
        }
    }
}