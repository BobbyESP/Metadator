/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.domain.editor.PositionStyle
import com.bobbyesp.metadator.core.domain.lookup.FieldProposal
import com.bobbyesp.metadator.core.domain.lookup.LookupOutcome
import com.bobbyesp.metadator.core.domain.lookup.LookupService
import com.bobbyesp.metadator.core.domain.lookup.proposalsFor
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.tags.api.TagMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LookupStatus {
    data object Idle : LookupStatus

    data object Searching : LookupStatus

    data class Results(val candidates: List<LookupCandidate>, val failedProviders: List<String>) :
        LookupStatus

    data object NoProviders : LookupStatus

    data object Offline : LookupStatus

    data object RateLimited : LookupStatus

    data object Failed : LookupStatus
}

/** One result opened for comparison: field by field, each with a checkbox. */
data class Comparison(
    val candidate: LookupCandidate,
    val proposals: List<FieldProposal>,
    val checked: Set<String>,
    val includeCover: Boolean,
) {
    val changedProposals: List<FieldProposal>
        get() = proposals.filter { it.differs }

    val selectedFields: Map<String, List<String>>
        get() = proposals.filter { it.key in checked }.associate { it.key to it.proposed }
}

data class LookupState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long? = null,
    val status: LookupStatus = LookupStatus.Idle,
    val comparison: Comparison? = null,
)

/**
 * The "Find metadata" sheet. It proposes; the editor applies. Nothing here touches a file: applying
 * copies the chosen values into the editor's draft, and only Save writes.
 */
class LookupViewModel(private val service: LookupService) : ViewModel() {
    private val _state = MutableStateFlow(LookupState())
    val state: StateFlow<LookupState> = _state.asStateFlow()
    private var search: Job? = null

    /** Fills the query from the song, once, and searches. */
    fun start(title: String, artist: String, album: String, durationMs: Long?) {
        _state.update { LookupState(title, artist, album, durationMs) }
        search()
    }

    fun setQuery(
        title: String = _state.value.title,
        artist: String = _state.value.artist,
        album: String = _state.value.album,
    ) {
        _state.update { it.copy(title = title, artist = artist, album = album) }
    }

    fun search() {
        val query = _state.value
        search?.cancel()
        search = viewModelScope.launch {
            _state.update { it.copy(status = LookupStatus.Searching, comparison = null) }
            val outcome =
                service.search(
                    LookupQuery(
                        title = query.title.trim(),
                        artist = query.artist.trim().ifEmpty { null },
                        album = query.album.trim().ifEmpty { null },
                        durationMs = query.durationMs,
                    )
                )
            _state.update {
                it.copy(
                    status =
                        when (outcome) {
                            is LookupOutcome.Results ->
                                LookupStatus.Results(outcome.candidates, outcome.failedProviders)
                            LookupOutcome.NoProviders -> LookupStatus.NoProviders
                            LookupOutcome.Offline -> LookupStatus.Offline
                            LookupOutcome.RateLimited -> LookupStatus.RateLimited
                            LookupOutcome.Failed -> LookupStatus.Failed
                        }
                )
            }
        }
    }

    /** Compares [candidate] with the editor's current tags. Fields that differ start checked. */
    fun open(candidate: LookupCandidate, current: TagMap, style: PositionStyle) {
        val proposals = proposalsFor(candidate, current, style)
        _state.update {
            it.copy(
                comparison =
                    Comparison(
                        candidate = candidate,
                        proposals = proposals,
                        checked =
                            proposals
                                .filter { proposal -> proposal.differs }
                                .mapTo(mutableSetOf()) { p -> p.key },
                        includeCover = candidate.artworkUrl != null,
                    )
            )
        }
    }

    fun toggle(key: String) = _state.update { state ->
        val comparison = state.comparison ?: return@update state
        state.copy(
            comparison =
                comparison.copy(
                    checked =
                        if (key in comparison.checked) comparison.checked - key
                        else comparison.checked + key
                )
        )
    }

    fun toggleCover() = _state.update { state ->
        val comparison = state.comparison ?: return@update state
        state.copy(comparison = comparison.copy(includeCover = !comparison.includeCover))
    }

    fun closeComparison() = _state.update { it.copy(comparison = null) }
}
