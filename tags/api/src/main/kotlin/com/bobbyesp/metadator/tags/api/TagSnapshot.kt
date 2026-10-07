package com.bobbyesp.metadator.tags.api

/** Everything that can be edited in a file at one moment: its tags and its pictures. */
data class TagSnapshot(val tags: TagMap, val pictures: List<EmbeddedPicture>) {
    val frontCover: EmbeddedPicture?
        get() =
            pictures.firstOrNull { it.type == PictureType.FrontCover }
                ?: pictures.firstOrNull()

    companion object {
        val Empty = TagSnapshot(TagMap.Empty, emptyList())
    }
}

/** The audio stream, read-only. */
data class AudioProperties(
    val durationMs: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
)
