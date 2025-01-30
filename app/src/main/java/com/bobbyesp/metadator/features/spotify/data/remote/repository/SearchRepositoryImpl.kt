package com.bobbyesp.metadator.features.spotify.data.remote.repository

import androidx.paging.Pager
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.repositories.SearchRepository
import com.bobbyesp.metadator.features.spotify.domain.services.search.SpotifySearchService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Implementation of the [SearchRepository] interface. This is the class that should be used to
 * search for tracks, albums, playlists, and artists on Spotify.
 *
 * ⚠️We are currently using the [SpotifySearchService] to search for tracks on Spotify. Using that
 * service directly is not recommended. Instead, we should use this repository to search for tracks,
 * but this implementation is not complete.
 *
 * @property searchService The service that will be used to search for tracks on Spotify.
 */
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

    override suspend fun searchAlbums(query: String): Result<List<Album>> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPlaylists(query: String): Result<List<Playlist>> {
        TODO("Not yet implemented")
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPaginatedTracks(query: String): Pager<Int, Track> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPaginatedAlbums(query: String): Pager<Int, Album> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPaginatedPlaylists(query: String): Pager<Int, Playlist> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPaginatedArtists(query: String): Pager<Int, Artist> {
        TODO("Not yet implemented")
    }
}