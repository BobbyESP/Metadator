package com.bobbyesp.metadator.features.spotify.data.repository

import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.repositories.SearchRepository
import com.bobbyesp.metadator.features.spotify.domain.services.search.SpotifySearchService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchRepositoryImpl : SearchRepository, KoinComponent {
    private val searchService by inject<SpotifySearchService>()

    /**
     * Search for tracks on Spotify. The search does support pagination but this implementation
     * does not support it. For now, it will only return up to 50 results.
     */
    override suspend fun searchTracks(query: String): Result<List<Track>> {
        try {
            val searchResult = searchService.search(
                query,
                searchTypes = arrayOf(SearchApi.SearchType.Track),
                filters = emptyList()
            )

            searchResult.tracks?.let { return Result.success(it.items) }
                ?: return Result.failure(NullPointerException("Search result is null"))
        } catch (th: Throwable) {
            return Result.failure(th)
        }
    }
}