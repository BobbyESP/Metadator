package com.bobbyesp.metadator.features.spotify.domain.repositories

import androidx.paging.Pager
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track

interface SearchRepository {
    suspend fun searchTracks(query: String): Result<List<Track>>

    suspend fun searchAlbums(query: String): Result<List<Album>>

    suspend fun searchPlaylists(query: String): Result<List<Playlist>>

    suspend fun searchArtists(query: String): Result<List<Artist>>

    suspend fun searchPaginatedTracks(query: String): Pager<Int, Track>

    suspend fun searchPaginatedAlbums(query: String): Pager<Int, Album>

    suspend fun searchPaginatedPlaylists(query: String): Pager<Int, Playlist>

    suspend fun searchPaginatedArtists(query: String): Pager<Int, Artist>
}
