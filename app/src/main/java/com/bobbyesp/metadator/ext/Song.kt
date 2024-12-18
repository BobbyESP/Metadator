package com.bobbyesp.metadator.ext

import com.bobbyesp.metadator.domain.model.ParcelableSong
import com.bobbyesp.utilities.model.Song

fun Song.toParcelableSong(): ParcelableSong {
    return ParcelableSong(
        name = this.title,
        mainArtist = this.artist,
        localPath = this.path,
        artworkPath = this.artworkPath,
        filename = this.fileName
    )
}