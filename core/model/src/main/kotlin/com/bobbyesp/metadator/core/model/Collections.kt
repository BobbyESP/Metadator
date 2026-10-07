package com.bobbyesp.metadator.core.model

/** A group of songs the library is browsed by. */
sealed interface TrackCollection {
    val key: String
    val title: String
    val tracks: List<Track>

    val artworkRef: ContentRef?
        get() = tracks.firstNotNullOfOrNull { it.artworkRef }

    data class Album(
        override val key: String,
        override val title: String,
        val artist: String?,
        val year: Int?,
        override val tracks: List<Track>,
    ) : TrackCollection

    data class Artist(override val key: String, override val title: String, override val tracks: List<Track>) :
        TrackCollection {
        val albumCount: Int
            get() = tracks.mapNotNull { it.album }.distinct().size
    }

    data class Folder(override val key: String, override val title: String, override val tracks: List<Track>) :
        TrackCollection
}

enum class CollectionType {
    Album,
    Artist,
    Folder,
}
