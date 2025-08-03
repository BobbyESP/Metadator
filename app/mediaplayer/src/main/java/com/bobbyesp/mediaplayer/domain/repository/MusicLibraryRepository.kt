package com.bobbyesp.mediaplayer.domain.repository

import android.net.Uri
import android.provider.MediaStore
import com.bobbyesp.mediaplayer.domain.enums.MediaStoreSearchFilter
import com.bobbyesp.mediaplayer.domain.model.Genre
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import kotlinx.coroutines.flow.Flow

interface MusicLibraryRepository {
    val musicUri: Uri
        get() = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun getMusicLibrary(
        query: String? = null,
        searchFilters: List<MediaStoreSearchFilter>? = null,
    ): List<MusicTrack>

    fun observeMusicLibrary(
        query: String?,
        searchFilters: List<MediaStoreSearchFilter>?,
    ): Flow<List<MusicTrack>>

    fun getGenres(): List<Genre>

    fun getTrackIdMapToGenreName(): Map<Long, String>

    fun getFoldersWithAudio(): Set<String>
}
