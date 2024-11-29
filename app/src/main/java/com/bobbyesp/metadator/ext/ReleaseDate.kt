package com.bobbyesp.metadator.ext

import com.adamratzman.spotify.models.ReleaseDate

fun ReleaseDate.format(precision: String?, separator: String = "/"): String {
    return when (precision) {
        "year" -> {
            "$year"
        }

        "month" -> {
            "$month $separator $year"
        }

        "day" -> {
            "$day $separator $month $separator $year"
        }

        else -> {
            "Unknown"
        }
    }
}