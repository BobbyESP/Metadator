package com.bobbyesp.metadator.core.ext

import com.adamratzman.spotify.models.SimpleArtist

fun <T> List<T>.formatArtists(useAmpersands: Boolean = false): String {
    val artistNames =
        when (firstOrNull()) {
            is String -> this as List<String>
            is SimpleArtist -> (this as List<SimpleArtist>).mapNotNull { it.name }
            else -> return ""
        }

    return if (useAmpersands) {
        when (artistNames.size) {
            0 -> ""
            1 -> artistNames.first()
            2 -> artistNames.joinToString(" & ")
            else ->
                artistNames.subList(0, artistNames.size - 1).joinToString(", ") +
                    " & " +
                    artistNames.last()
        }
    } else {
        artistNames.joinToString(", ")
    }
}
