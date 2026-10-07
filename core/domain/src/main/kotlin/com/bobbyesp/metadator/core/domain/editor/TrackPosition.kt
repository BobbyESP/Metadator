package com.bobbyesp.metadator.core.domain.editor

import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap

/**
 * A track or disc position and its total, as text: "A1" on a vinyl rip is a valid position, and
 * turning it into a number would lose it.
 */
data class Position(val number: String = "", val total: String = "") {
    val isEmpty: Boolean
        get() = number.isBlank() && total.isBlank()
}

/**
 * How a format stores totals. ID3 and MP4 keep "3/12" in one field; Vorbis comments (FLAC, Ogg,
 * Opus) use a separate TRACKTOTAL, and a "3/12" there confuses most players.
 */
enum class PositionStyle {
    Combined,
    Separate,
}

object TrackPositions {
    private val VorbisExtensions = setOf("flac", "ogg", "oga", "opus", "spx")

    fun detectStyle(tags: TagMap, fileExtension: String?): PositionStyle =
        when {
            (TagField.TrackTotalKeys + TagField.DiscTotalKeys).any { it in tags } ->
                PositionStyle.Separate
            fileExtension?.lowercase() in VorbisExtensions -> PositionStyle.Separate
            else -> PositionStyle.Combined
        }

    fun read(tags: TagMap, field: TagField): Position {
        val raw = tags.first(field.key).orEmpty().trim()
        val separateTotal = totalKeys(field).firstNotNullOfOrNull { tags.first(it) }?.trim()
        return if ('/' in raw) {
            val (number, total) = raw.split('/', limit = 2).map { it.trim() }
            Position(number, separateTotal ?: total)
        } else {
            Position(raw, separateTotal.orEmpty())
        }
    }

    /** The field values that store [position], as changes for [tags]. */
    fun write(
        tags: TagMap,
        field: TagField,
        position: Position,
        style: PositionStyle,
    ): Map<String, List<String>> {
        val number = position.number.trim()
        val total = position.total.trim()
        val keys = totalKeys(field)
        return when (style) {
            PositionStyle.Combined ->
                buildMap {
                    put(
                        field.key,
                        listOfNotNull(
                            when {
                                number.isEmpty() -> null
                                total.isEmpty() -> number
                                else -> "$number/$total"
                            }
                        ),
                    )
                    keys.filter { it in tags }.forEach { put(it, emptyList()) }
                }
            PositionStyle.Separate ->
                buildMap {
                    put(field.key, listOfNotNull(number.ifEmpty { null }))
                    val totalKey = keys.firstOrNull { it in tags } ?: keys.first()
                    put(totalKey, listOfNotNull(total.ifEmpty { null }))
                    keys.filter { it != totalKey && it in tags }.forEach { put(it, emptyList()) }
                }
        }
    }

    private fun totalKeys(field: TagField): List<String> =
        when (field) {
            TagField.TrackNumber -> TagField.TrackTotalKeys
            TagField.DiscNumber -> TagField.DiscTotalKeys
            else -> error("$field is not a position")
        }
}
