package com.bobbyesp.mediaplayer.ext

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun MediaMetadata.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaMetadata(this)
        .build()
}