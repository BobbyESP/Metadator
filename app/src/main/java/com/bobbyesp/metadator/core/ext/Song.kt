package com.bobbyesp.metadator.core.ext

import com.bobbyesp.metadator.core.domain.model.ParcelableSong
import com.bobbyesp.utilities.mediastore.model.Song

fun Song.toParcelableSong(): ParcelableSong {
    return ParcelableSong(
        name = this.title,
        mainArtist = this.artist,
        localPath = this.path,
        artworkPath = this.artworkPath,
        filename = this.fileName,
    )
}
