package com.bobbyesp.metadator.ext

import com.adamratzman.spotify.models.SimpleArtist

fun List<String>.formatArtists(): String {
    return when (size) {
        0 -> ""
        1 -> first()
        2 -> joinToString(" & ")
        else -> {
            val last = last()
            val allButLast = subList(0, size - 1).joinToString(", ")
            "$allButLast & $last"
        }
    }
}

fun List<SimpleArtist>.formatArtistsName(): String {
    return when (size) {
        0 -> ""
        1 -> first().name ?: ""
        2 -> joinToString(" & ") { it.name ?: "" }
        else -> {
            val last = last().name ?: ""
            val allButLast = subList(0, size - 1).joinToString(", ") { it.name ?: "" }
            "$allButLast & $last"
        }
    }
}