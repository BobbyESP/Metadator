package com.bobbyesp.metadator.ext

import com.adamratzman.spotify.models.SimpleArtist

fun <T> List<T>.formatArtists(useAnds: Boolean = false): String {
    val names = when (firstOrNull()) {
        is String -> this as List<String>
        is SimpleArtist -> (this as List<SimpleArtist>).mapNotNull { it.name }
        else -> return ""
    }

    return if (useAnds) {
        when (names.size) {
            0 -> ""
            1 -> names.first()
            2 -> names.joinToString(" & ")
            else -> names.subList(0, names.size - 1).joinToString(", ") + " & " + names.last()
        }
    } else {
        names.joinToString(", ")
    }
}