package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.services.search.SpotifySearchService
import com.bobbyesp.utilities.states.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class MetadataBsVM(
    private val searchService: SpotifySearchService
) : KoinComponent, ViewModel() {
    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    private val _outerEventsFlow = MutableSharedFlow<OuterEvent>()
    val outerEventsFlow = _outerEventsFlow.asSharedFlow()

    data class ViewState(
        val stage: BottomSheetStage = BottomSheetStage.SEARCH,
        val searchedTracks: ResourceState<Flow<PagingData<Track>>> = ResourceState.Loading(null),
        val selectedTrack: Track? = null,
        val lastQuery: String = "",
    )

    private fun searchTracks(query: String) {
        updateQuery(query)
        viewModelScope.launch(Dispatchers.IO) {
            getTracksPaginatedData(query)
        }
    }

    private suspend fun getTracksPaginatedData(query: String) {
        try {
            val tracksPager = searchService.searchPaginatedTracks(
                query = query,
                filters = emptyList()
            ).flow.cachedIn(viewModelScope)

            mutableViewStateFlow.update {
                it.copy(
                    searchedTracks = ResourceState.Success(tracksPager)
                )
            }
        } catch (th: Throwable) {
            mutableViewStateFlow.update {
                it.copy(
                    searchedTracks = ResourceState.Error(
                        errorMessage = th.message ?: th.stackTrace.toString()
                    )
                )
            }
        }
    }

    private fun chooseTrack(track: Track?) {
        mutableViewStateFlow.update {
            it.copy(
                selectedTrack = track
            )
        }
        updateStage(BottomSheetStage.TRACK_DETAILS)
    }

    private fun updateStage(stage: BottomSheetStage) {
        mutableViewStateFlow.update {
            it.copy(
                stage = stage
            )
        }
    }

    private fun saveMetadata(modifiedFields: Map<String, String>) {
        viewModelScope.launch {
            _outerEventsFlow.emit(OuterEvent.SaveMetadata(modifiedFields))
        }
    }

    private fun updateQuery(query: String) {
        mutableViewStateFlow.update {
            it.copy(
                lastQuery = query
            )
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.Search -> {
                searchTracks(event.query)
            }

            is Event.ChangeState -> {
                updateStage(event.state)
            }

            is Event.SelectTrack -> {
                chooseTrack(event.track)
                if (event.track == null) updateStage(BottomSheetStage.SEARCH)
            }

            is Event.UpdateMetadataFields -> {
                saveMetadata(event.properties)
            }
        }
    }

    interface OuterEvent {
        data class SaveMetadata(val modifiedFields: Map<String, String>) : OuterEvent
    }

    interface Event {
        data class Search(val query: String) : Event
        data class ChangeState(val state: BottomSheetStage) : Event
        data class SelectTrack(val track: Track?) : Event
        data class UpdateMetadataFields(val properties: Map<String, String>) : Event
    }

    companion object {
        enum class BottomSheetStage {
            SEARCH,
            TRACK_DETAILS
        }
    }
}