package com.bobbyesp.metadator.ext

import com.adamratzman.spotify.models.ReleaseDate

fun ReleaseDate.format(precision: String?): String {
    return when (precision) {
        "year" -> {
            "$year"
        }

        "month" -> {
            "$month - $year"
        }

        "day" -> {
            "$day-$month-$year"
        }

        else -> {
            "Unknown"
        }
    }
}