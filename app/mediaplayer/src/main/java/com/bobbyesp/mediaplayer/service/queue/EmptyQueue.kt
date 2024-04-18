package com.bobbyesp.mediaplayer.service.queue

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object EmptyQueue : Queue {
    override val preloadItem: MediaMetadata?
        get() = null

    override suspend fun getInitialData(): Queue.Data = Queue.Data.empty()
    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}