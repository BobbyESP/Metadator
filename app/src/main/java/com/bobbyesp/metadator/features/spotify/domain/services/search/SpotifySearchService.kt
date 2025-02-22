package com.bobbyesp.metadator.features.spotify.domain.services.search

import androidx.paging.Pager
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.SearchFilter
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track

interface SpotifySearchService {
  suspend fun search(
      query: String,
      vararg searchTypes: SearchApi.SearchType,
      filters: List<SearchFilter>
  ): SpotifySearchResult

  suspend fun searchPaginatedTracks(
      query: String,
      filters: List<SearchFilter>,
  ): Pager<Int, Track>
}
