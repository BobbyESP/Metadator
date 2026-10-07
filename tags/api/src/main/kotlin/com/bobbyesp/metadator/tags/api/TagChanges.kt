package com.bobbyesp.metadator.tags.api

/**
 * What the user changed, and nothing else.
 *
 * A save applies this to the file as it is at that moment ([TagSnapshot.applying]), so every key
 * the user did not touch is written back exactly as it was read, including the ones Metadator has
 * no name for. Writing a whole map built from a handful of known fields is what deleted ReplayGain,
 * MusicBrainz ids and every other tag in 1.x.
 */
data class TagChanges(
    /** New values by key. An empty list removes the key. */
    val fields: Map<String, List<String>> = emptyMap(),
    val artwork: ArtworkChange = ArtworkChange.Unchanged,
) {
    val isEmpty: Boolean
        get() = fields.isEmpty() && artwork == ArtworkChange.Unchanged

    val changedKeys: Set<String>
        get() = fields.keys.mapTo(mutableSetOf(), TagMap::normalizeKey)

    operator fun plus(other: TagChanges): TagChanges =
        TagChanges(
            fields = fields + other.fields,
            artwork = if (other.artwork != ArtworkChange.Unchanged) other.artwork else artwork,
        )
}

sealed interface ArtworkChange {
    /** Pictures are not written at all. */
    data object Unchanged : ArtworkChange

    /** The front cover is replaced; every other picture (back cover, artist…) is kept. */
    data class Replace(val picture: EmbeddedPicture) : ArtworkChange

    /** The front cover is removed; every other picture is kept. */
    data object Remove : ArtworkChange
}

/** The tags after [changes]; untouched keys keep their values exactly. */
fun TagMap.applying(changes: Map<String, List<String>>): TagMap =
    changes.entries.fold(this) { map, (key, values) -> map.with(key, values) }

/** The pictures after [change], or null when they must not be written. */
fun List<EmbeddedPicture>.applying(change: ArtworkChange): List<EmbeddedPicture>? =
    when (change) {
        ArtworkChange.Unchanged -> null
        ArtworkChange.Remove -> filterNot { it.isFrontCover(this) }
        is ArtworkChange.Replace -> {
            val cover = change.picture.copy(type = PictureType.FrontCover)
            listOf(cover) + filterNot { it.isFrontCover(this) }
        }
    }

/**
 * A file with a single untyped picture (MP4 has no types) treats it as its cover: otherwise
 * replacing the cover would add a second picture instead.
 */
private fun EmbeddedPicture.isFrontCover(all: List<EmbeddedPicture>): Boolean =
    type == PictureType.FrontCover || (all.size == 1 && type == PictureType.Other)

data class AppliedSnapshot(val tags: TagMap, val pictures: List<EmbeddedPicture>?)

fun TagSnapshot.applying(changes: TagChanges): AppliedSnapshot =
    AppliedSnapshot(tags = tags.applying(changes.fields), pictures = pictures.applying(changes.artwork))

/**
 * The changes that turn [original] into [edited]: only the keys whose values differ. Comparing
 * values, not "was the field focused", means typing a title and typing it back is no change.
 */
fun diff(original: TagMap, edited: TagMap): Map<String, List<String>> = buildMap {
    (original.keys + edited.keys).forEach { key ->
        val before = original[key]
        val after = edited[key]
        if (before != after) put(key, after)
    }
}
