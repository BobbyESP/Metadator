package com.bobbyesp.metadator.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.common.StringProvider
import com.bobbyesp.metadator.core.domain.library.LibraryFilter
import com.bobbyesp.metadator.core.domain.library.albums
import com.bobbyesp.metadator.core.domain.library.artists
import com.bobbyesp.metadator.core.domain.library.filtered
import com.bobbyesp.metadator.core.domain.library.folders
import com.bobbyesp.metadator.core.domain.library.sorted
import com.bobbyesp.metadator.core.model.SortOrder
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackCollection
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.core.model.TrackSort
import com.bobbyesp.metadator.core.ui.viewmodel.BaseViewModel
import com.bobbyesp.metadator.core.ui.viewmodel.UiMessage
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.player.api.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

enum class LibraryTab {
    Songs,
    Albums,
    Artists,
    Folders,
}

data class LibraryState(
    val loading: Boolean = true,
    val totalTracks: Int = 0,
    val tracks: List<Track> = emptyList(),
    val albums: List<TrackCollection.Album> = emptyList(),
    val artists: List<TrackCollection.Artist> = emptyList(),
    val folders: List<TrackCollection.Folder> = emptyList(),
    val tab: LibraryTab = LibraryTab.Songs,
    val filter: LibraryFilter = LibraryFilter(),
    val sort: SortOrder = SortOrder(),
    val formats: List<String> = emptyList(),
    val needsAttentionCount: Int = 0,
    val selection: Set<TrackId> = emptySet(),
    val refreshing: Boolean = false,
    val playingId: TrackId? = null,
    val playerActive: Boolean = false,
) {
    val selecting: Boolean
        get() = selection.isNotEmpty()

    val selectedTracks: List<Track>
        get() = tracks.filter { it.id in selection }
}

sealed interface LibraryIntent {
    data object Start : LibraryIntent

    data class Search(val query: String) : LibraryIntent

    data class SelectTab(val tab: LibraryTab) : LibraryIntent

    data object ToggleNeedsAttention : LibraryIntent

    data class ToggleFormat(val format: String) : LibraryIntent

    data class Sort(val sort: TrackSort) : LibraryIntent

    data object ToggleSortDirection : LibraryIntent

    data object ClearFilters : LibraryIntent

    data class ToggleSelection(val id: TrackId) : LibraryIntent

    data object SelectAll : LibraryIntent

    data object ClearSelection : LibraryIntent

    data object Refresh : LibraryIntent

    data class Play(val track: Track) : LibraryIntent

    data class PlayAll(val shuffle: Boolean) : LibraryIntent

    data object PlaySelection : LibraryIntent
}

class LibraryViewModel(
    private val library: AudioLibrary,
    private val player: PlayerController,
    private val strings: StringProvider,
    private val dispatchers: AppDispatchers,
    private val savedState: SavedStateHandle,
) : BaseViewModel<LibraryIntent, LibraryState, Nothing>(
    LibraryState(
        tab = savedState.get<String>(KEY_TAB)?.let { name -> LibraryTab.entries.firstOrNull { it.name == name } } ?: LibraryTab.Songs,
        filter = LibraryFilter(search = savedState[KEY_SEARCH] ?: ""),
    )
) {
    private val query = MutableStateFlow(Query(currentState.filter, currentState.sort))
    private var observing: Job? = null

    private data class Query(val filter: LibraryFilter, val sort: SortOrder)

    init {
        player.state
            .onEach { playback ->
                setState { copy(playingId = playback.current?.id, playerActive = playback.isActive) }
            }
            .launchIn(viewModelScope)
    }

    override fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.Start -> startObserving()
            is LibraryIntent.Search -> {
                savedState[KEY_SEARCH] = intent.query
                updateQuery { copy(filter = filter.copy(search = intent.query)) }
            }
            is LibraryIntent.SelectTab -> {
                savedState[KEY_TAB] = intent.tab.name
                setState { copy(tab = intent.tab, selection = emptySet()) }
            }
            LibraryIntent.ToggleNeedsAttention ->
                updateQuery { copy(filter = filter.copy(needsAttention = !filter.needsAttention)) }
            is LibraryIntent.ToggleFormat ->
                updateQuery {
                    val formats = filter.formats
                    copy(
                        filter =
                            filter.copy(
                                formats =
                                    if (intent.format in formats) formats - intent.format
                                    else formats + intent.format
                            )
                    )
                }
            is LibraryIntent.Sort -> updateQuery { copy(sort = sort.copy(sort = intent.sort)) }
            LibraryIntent.ToggleSortDirection ->
                updateQuery { copy(sort = sort.copy(ascending = !sort.ascending)) }
            LibraryIntent.ClearFilters ->
                updateQuery { copy(filter = LibraryFilter(search = filter.search)) }
            is LibraryIntent.ToggleSelection ->
                setState {
                    copy(selection = if (intent.id in selection) selection - intent.id else selection + intent.id)
                }
            LibraryIntent.SelectAll -> setState { copy(selection = tracks.mapTo(mutableSetOf()) { it.id }) }
            LibraryIntent.ClearSelection -> setState { copy(selection = emptySet()) }
            LibraryIntent.Refresh -> refresh()
            is LibraryIntent.Play -> {
                val queue = currentState.tracks
                player.play(queue, startIndex = queue.indexOf(intent.track).coerceAtLeast(0))
            }
            is LibraryIntent.PlayAll -> player.play(currentState.tracks, 0, shuffle = intent.shuffle)
            LibraryIntent.PlaySelection -> {
                player.play(currentState.selectedTracks)
                setState { copy(selection = emptySet()) }
            }
        }
    }

    private fun updateQuery(transform: Query.() -> Query) {
        query.update(transform)
        setState { copy(filter = query.value.filter, sort = query.value.sort) }
    }

    private fun startObserving() {
        if (observing?.isActive == true) return
        observing =
            combine(library.observeTracks(), query) { tracks, query -> present(tracks, query) }
                .flowOn(dispatchers.default)
                .catch {
                    setState { copy(loading = false) }
                    showMessage(UiMessage(strings.get(R.string.library_load_failed)))
                }
                .onEach { presented -> setState { presented(this) } }
                .launchIn(viewModelScope)
    }

    /** Everything derived from the library and the query, computed off the main thread. */
    private fun present(all: List<Track>, query: Query): LibraryState.() -> LibraryState {
        val visible = all.filtered(query.filter).sorted(query.sort)
        val albums = visible.albums()
        val artists = visible.artists()
        val folders = visible.folders()
        val formats = all.map { it.fileExtension }.filter { it.isNotEmpty() }.distinct().sorted()
        val needsAttention = all.count { it.needsAttention }
        val visibleIds = visible.mapTo(mutableSetOf()) { it.id }
        return {
            copy(
                loading = false,
                totalTracks = all.size,
                tracks = visible,
                albums = albums,
                artists = artists,
                folders = folders,
                formats = formats,
                needsAttentionCount = needsAttention,
                selection = selection.filterTo(mutableSetOf()) { it in visibleIds },
            )
        }
    }

    private fun refresh() {
        launch {
            setState { copy(refreshing = true) }
            try {
                library.refresh()
            } finally {
                setState { copy(refreshing = false) }
            }
        }
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_SEARCH = "search"
    }
}
