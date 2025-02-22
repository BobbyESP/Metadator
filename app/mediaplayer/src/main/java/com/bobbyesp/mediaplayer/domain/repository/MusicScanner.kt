package com.bobbyesp.mediaplayer.domain.repository

import android.net.Uri
import android.provider.MediaStore
import com.bobbyesp.mediaplayer.domain.enums.MediaStoreSearchFilter
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import kotlinx.coroutines.flow.Flow

interface MusicScanner {
    val musicUri: Uri
        get() = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun getMusicLibrary(
        searchQuery: String? = null,
        filters: List<MediaStoreSearchFilter>? = null,
    ): List<MusicTrack>

    fun observeMusicLibrary(
        searchQuery: String?,
        filters: List<MediaStoreSearchFilter>?,
    ): Flow<List<MusicTrack>>
}
