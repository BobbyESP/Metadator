package com.bobbyesp.mediaplayer.service.queue

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?
    suspend fun getInitialData(): Data
    fun hasNextPage(): Boolean
    suspend fun nextPage(): List<MediaItem>

    data class Data(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        companion object {
            fun empty() = Data(null, emptyList(), -1, 0L)
        }
    }
}