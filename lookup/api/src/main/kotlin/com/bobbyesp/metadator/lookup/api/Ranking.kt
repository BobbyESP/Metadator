/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.api

import com.bobbyesp.metadator.core.common.similarity
import kotlin.math.abs

/**
 * Orders [candidates] by how well they match [query], best first, with the score filled in.
 *
 * Title weighs most, then artist, then album; duration breaks ties between versions of one song
 * (album cut, radio edit, live), which share a title and an artist.
 */
fun rank(query: LookupQuery, candidates: List<LookupCandidate>): List<LookupCandidate> =
    candidates.map { it.copy(score = score(query, it)) }.sortedByDescending { it.score }

internal fun score(query: LookupQuery, candidate: LookupCandidate): Double {
    var total = 0.0
    var weights = 0.0

    fun add(weight: Double, value: Double) {
        total += weight * value
        weights += weight
    }

    if (query.title.isNotBlank()) add(0.5, similarity(query.title, candidate.title))
    if (!query.artist.isNullOrBlank()) {
        val best =
            (candidate.artists + candidate.artists.joinToString(" ")).maxOfOrNull {
                similarity(query.artist, it)
            } ?: 0.0
        add(0.3, best)
    }
    if (!query.album.isNullOrBlank() && candidate.album != null) {
        add(0.1, similarity(query.album, candidate.album))
    }
    if (query.durationMs != null && candidate.durationMs != null) {
        val seconds = abs(query.durationMs - candidate.durationMs) / 1000.0
        // Within 2 s is the same recording; past 20 s it almost certainly is not.
        add(0.1, (1.0 - (seconds - 2.0).coerceAtLeast(0.0) / 18.0).coerceIn(0.0, 1.0))
    }
    return if (weights == 0.0) 0.0 else total / weights
}

/** How sure the app is about a candidate, for its label. */
enum class MatchConfidence {
    High,
    Medium,
    Low;

    companion object {
        fun of(score: Double): MatchConfidence =
            when {
                score >= 0.85 -> High
                score >= 0.6 -> Medium
                else -> Low
            }
    }
}
