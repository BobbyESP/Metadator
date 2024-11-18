package com.bobbyesp.metadator.features.spotify.data.remote.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.SearchFilter
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.pagination.TracksPagingSource
import com.bobbyesp.metadator.features.spotify.domain.services.SpotifyService
import com.bobbyesp.metadator.features.spotify.domain.services.search.SpotifySearchService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpotifySearchServiceImpl : SpotifySearchService, KoinComponent {
    private val spotifyService by inject<SpotifyService>()

    override suspend fun search(
        query: String,
        vararg searchTypes: SearchApi.SearchType,
        filters: List<SearchFilter>
    ): SpotifySearchResult {
        val api = spotifyService.getSpotifyApi()
        return api.search.search(query = query, searchTypes = searchTypes, filters = filters)
    }

    override suspend fun searchPaginatedTracks(
        query: String,
        filters: List<SearchFilter>
    ): Pager<Int, Track> {
        val api = spotifyService.getSpotifyApi()
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 40,
            ),
            pagingSourceFactory = {
                TracksPagingSource(
                    spotifyApi = api,
                    query = query,
                )
            }
        )
    }
}