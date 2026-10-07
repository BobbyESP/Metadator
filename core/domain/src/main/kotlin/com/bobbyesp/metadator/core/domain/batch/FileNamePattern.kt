/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.batch

import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap

/**
 * A file name pattern such as `{track} - {artist} - {title}`, for reading tags out of file names.
 *
 * Placeholders are matched lazily and the literal text between them exactly, so `{artist} -
 * {title}` reads "AC/DC - Back In Black - Live" as artist "AC/DC" and title "Back In Black - Live".
 */
class FileNamePattern(val pattern: String) {

    val placeholders: List<Placeholder> =
        PLACEHOLDER.findAll(pattern).mapNotNull { Placeholder.forName(it.groupValues[1]) }.toList()

    val isValid: Boolean
        get() =
            placeholders.isNotEmpty() &&
                PLACEHOLDER.findAll(pattern).count() == placeholders.size &&
                placeholders.size == placeholders.distinct().size

    private val regex: Regex? by lazy {
        if (!isValid) return@lazy null
        val builder = StringBuilder("^")
        var last = 0
        PLACEHOLDER.findAll(pattern).forEachIndexed { index, match ->
            builder.append(Regex.escape(pattern.substring(last, match.range.first)))
            val isLast = index == placeholders.lastIndex
            builder.append(if (isLast && match.range.last == pattern.lastIndex) "(.+)" else "(.+?)")
            last = match.range.last + 1
        }
        builder.append(Regex.escape(pattern.substring(last))).append('$')
        Regex(builder.toString())
    }

    /** The tags in [fileName] (extension ignored), or null when it does not fit the pattern. */
    fun parse(fileName: String): Map<String, List<String>>? {
        val name = fileName.substringBeforeLast('.', missingDelimiterValue = fileName).trim()
        val match = regex?.matchEntire(name) ?: return null
        return placeholders
            .mapIndexed { index, placeholder ->
                placeholder.key to listOf(match.groupValues[index + 1].trim())
            }
            .filter { (_, values) -> values.first().isNotEmpty() }
            .toMap()
    }

    /** The file name [tags] would give, without extension, or null if a placeholder is empty. */
    fun format(tags: TagMap): String? {
        var missing = false
        val result =
            PLACEHOLDER.replace(pattern) { match ->
                val placeholder = Placeholder.forName(match.groupValues[1])
                val raw = placeholder?.let { tags.first(it.key) }
                // "3/12" names a file "3", but "AC/DC" stays an artist.
                val value =
                    if (placeholder == Placeholder.Track || placeholder == Placeholder.Disc) {
                        raw?.substringBefore('/')
                    } else raw
                if (value.isNullOrBlank()) {
                    missing = true
                    ""
                } else value.replace(ILLEGAL_FILE_NAME_CHARS, "_")
            }
        return if (missing) null else result.trim()
    }

    enum class Placeholder(val token: String, val key: String) {
        Title("title", TagField.Title.key),
        Artist("artist", TagField.Artist.key),
        Album("album", TagField.Album.key),
        AlbumArtist("albumartist", TagField.AlbumArtist.key),
        Track("track", TagField.TrackNumber.key),
        Disc("disc", TagField.DiscNumber.key),
        Year("year", TagField.Date.key),
        Genre("genre", TagField.Genre.key);

        companion object {
            fun forName(name: String): Placeholder? = entries.firstOrNull {
                it.token.equals(name.trim(), ignoreCase = true)
            }
        }
    }

    companion object {
        // Both braces escaped: Android's regex engine (ICU) rejects a bare closing one, which
        // the JVM the tests run on accepts.
        private val PLACEHOLDER = Regex("""\{([^{}]+)\}""")
        private val ILLEGAL_FILE_NAME_CHARS = Regex("""[\\/:*?"<>|]""")

        val Suggestions =
            listOf(
                "{artist} - {title}",
                "{track} - {title}",
                "{track}. {title}",
                "{track} - {artist} - {title}",
                "{artist} - {album} - {track} - {title}",
            )
    }
}
