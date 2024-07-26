package com.bobbyesp.metadator.ext

import androidx.media3.common.MediaItem
import com.bobbyesp.utilities.model.Song

fun MediaItem.toSong(): Song {
    val mediaMetadata =
        this.mediaMetadata
    return Song(
        id = mediaId.hashCode().toLong(),
        title = (mediaMetadata.displayTitle ?: "").toString(),
        artist = (mediaMetadata.artist ?: "").toString(),
        album = (mediaMetadata.albumTitle ?: "").toString(),
        artworkPath = mediaMetadata.artworkUri,
        duration = 0.0,
        path = this.localConfiguration?.uri.toString(),
        fileName = (mediaMetadata.title ?: "").toString()
    )
}