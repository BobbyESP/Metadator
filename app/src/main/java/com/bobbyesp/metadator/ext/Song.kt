package com.bobbyesp.metadator.ext

import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.utilities.model.Song

fun Song.toParcelableSong(): ParcelableSong {
    val artistsList = this.artist.toList()
    val mainArtist = artistsList.first().toString()
    return ParcelableSong(
        name = this.title,
        mainArtist = mainArtist,
        localSongPath = this.path,
        artworkPath = this.artworkPath,
        fileName = this.fileName
    )
}