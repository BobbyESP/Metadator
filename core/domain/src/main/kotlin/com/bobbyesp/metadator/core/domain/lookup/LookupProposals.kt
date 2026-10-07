package com.bobbyesp.metadator.core.domain.lookup

import com.bobbyesp.metadator.core.domain.editor.Position
import com.bobbyesp.metadator.core.domain.editor.PositionStyle
import com.bobbyesp.metadator.core.domain.editor.TrackPositions
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap

/** One field of a lookup result next to what the file has. */
data class FieldProposal(
    val key: String,
    val field: TagField?,
    val current: List<String>,
    val proposed: List<String>,
) {
    val differs: Boolean
        get() = current != proposed
}

/**
 * What [candidate] would set in a file whose tags are [current], field by field. Only fields the
 * candidate knows are proposed: a provider without a genre never proposes removing one.
 */
fun proposalsFor(
    candidate: LookupCandidate,
    current: TagMap,
    positionStyle: PositionStyle,
): List<FieldProposal> {
    val values = linkedMapOf<String, List<String>>()

    fun put(field: TagField, value: List<String>) {
        if (value.isNotEmpty()) values[field.key] = value
    }

    put(TagField.Title, listOf(candidate.title).filter { it.isNotBlank() })
    put(TagField.Artist, candidate.artists)
    candidate.album?.let { put(TagField.Album, listOf(it)) }
    put(TagField.AlbumArtist, candidate.albumArtists)
    candidate.date?.let { put(TagField.Date, listOf(it)) }
    put(TagField.Genre, candidate.genres)
    candidate.isrc?.let { put(TagField.Isrc, listOf(it)) }
    candidate.label?.let { put(TagField.Label, listOf(it)) }

    if (candidate.trackNumber != null) {
        values +=
            TrackPositions.write(
                current,
                TagField.TrackNumber,
                Position(
                    candidate.trackNumber.toString(),
                    candidate.trackTotal?.toString().orEmpty(),
                ),
                positionStyle,
            )
    }
    if (candidate.discNumber != null) {
        values +=
            TrackPositions.write(
                current,
                TagField.DiscNumber,
                Position(candidate.discNumber.toString(), candidate.discTotal?.toString().orEmpty()),
                positionStyle,
            )
    }
    candidate.identifiers.forEach { (key, value) -> values[key] = listOf(value) }

    return values
        .filterValues { it.isNotEmpty() }
        .map { (key, proposed) ->
            FieldProposal(
                key = key,
                field = TagField.forKey(key),
                current = current[key],
                proposed = proposed,
            )
        }
}
