package com.bobbyesp.mediaplayer.service.queue

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class SongsQueue(
    val title: String? = null,
    val items: List<MediaItem>,
    val startIndex: Int = 0,
    val position: Long = 0L,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    override suspend fun getInitialData(): Queue.Data =
        Queue.Data(title, items, startIndex, position)

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage() = throw UnsupportedOperationException()
}
