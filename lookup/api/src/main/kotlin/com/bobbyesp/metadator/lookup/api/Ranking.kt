/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.api

import com.bobbyesp.metadator.core.common.normalizedForComparison
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Orders [candidates] by how well they match [query], best first, with the score filled in.
 *
 * Only what every provider can tell is weighed, so no provider is favoured:
 * - title, artist and album, compared without the parts that only decorate them ("(Remastered)",
 *   "(Deluxe Edition)", "feat. …"), and with word order not mattering;
 * - every credited artist, featured ones included, wherever they are written;
 * - duration, which tells apart versions of one song (album cut, radio edit, live);
 * - the version a title names: a live, karaoke, remix or cover recording is a poor match for a song
 *   that is not one, and the other way round;
 * - compilations, which lose to the original release unless the query asks for them;
 * - other providers listing the same recording, which makes it likelier to be the right one.
 *
 * A query without an artist may have it in the title ("Queen - Bohemian Rhapsody", as file names
 * go), and a title may start with its track number ("01. Halo"): both readings are tried.
 *
 * Ties go to the candidate with the most metadata, then to the earliest release.
 */
fun rank(query: LookupQuery, candidates: List<LookupCandidate>): List<LookupCandidate> {
    val readings = readingsOf(query)
    val views = candidates.map(::CandidateView)
    val support = corroboration(views)
    return views
        .mapIndexed { index, view ->
            val match = bestMatch(readings, view)
            val bonus =
                if (match >= CORROBORATION_FLOOR)
                    support[index].coerceAtMost(MAX_CORROBORATIONS) * CORROBORATION_BONUS
                else 0.0
            Ranked(view, match + bonus)
        }
        .sortedWith(
            compareByDescending<Ranked> { (it.raw * 100).roundToInt() }
                .thenByDescending { it.view.richness }
                .thenBy { it.view.candidate.date ?: "9999" }
        )
        .map { it.view.candidate.copy(score = it.raw.coerceIn(0.0, 1.0)) }
}

internal fun score(query: LookupQuery, candidate: LookupCandidate): Double =
    bestMatch(readingsOf(query), CandidateView(candidate)).coerceIn(0.0, 1.0)

private class Ranked(val view: CandidateView, val raw: Double)

private class QueryReading(
    title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val trust: Double,
) {
    val title = parseTitle(title)
    val artists =
        (artist?.let(::splitArtists).orEmpty() + this.title.featured)
            .distinctBy { it.normalizedForComparison() }
            .ifEmpty { listOfNotNull(artist) }
    val albumCore = album?.let(::albumCore)
    val variants = this.title.variants + markersIn(album) + markersIn(artist)
    val detailWords = detailWords(this.title.decorations)
    val compilation = isCompilation(album, emptyList())
}

private class CandidateView(val candidate: LookupCandidate) {
    val title = parseTitle(candidate.title)
    val coreKey = title.core.normalizedForComparison()
    val artists =
        (candidate.artists.flatMap(::splitArtists) + title.featured).distinctBy {
            it.normalizedForComparison()
        }
    val albumCore = candidate.album?.let(::albumCore)

    /** What the provider says the recording is, as opposed to the release it is on. */
    val recordingVariants = title.variants + versionVariants(candidate.version)
    val variants =
        recordingVariants +
            releaseVariants(
                candidate.releaseTypes,
                recordingDescribed = candidate.version != null,
            ) +
            markersIn(candidate.album) +
            candidate.artists.flatMap(::markersIn)
    val compilation = isCompilation(candidate.album, candidate.albumArtists, candidate.releaseTypes)
    val detailWords = detailWords(title.decorations + candidate.version + candidate.album)
    val richness =
        with(candidate) {
            listOf(
                    album != null,
                    date != null,
                    date != null && date.length >= 10,
                    trackNumber != null,
                    isrc != null,
                    artworkUrl != null,
                    durationMs != null,
                    genres.isNotEmpty(),
                    albumArtists.isNotEmpty(),
                )
                .count { it }
        }
}

private fun readingsOf(query: LookupQuery): List<QueryReading> {
    val artist = query.artist?.takeIf { it.isNotBlank() }
    val album = query.album?.takeIf { it.isNotBlank() }
    val titles =
        listOf(query.title, query.title.replace(TRACK_NUMBER, ""))
            .filter { it.isNotBlank() }
            .distinct()
    return buildList {
        titles.forEach { add(QueryReading(it, artist, album, query.durationMs, 1.0)) }
        if (artist != null) return@buildList
        for (title in titles) {
            val dash = DASH.find(title) ?: continue
            val left = title.substring(0, dash.range.first).trim()
            val right = title.substring(dash.range.last + 1).trim()
            if (left.isEmpty() || right.isEmpty()) continue
            add(QueryReading(right, left, album, query.durationMs, ARTIST_IN_TITLE_TRUST))
            add(QueryReading(left, right, album, query.durationMs, ARTIST_IN_TITLE_TRUST - 0.05))
        }
    }
}

private fun bestMatch(readings: List<QueryReading>, candidate: CandidateView): Double =
    readings.maxOfOrNull { match(it, candidate) } ?: 0.0

private fun match(query: QueryReading, candidate: CandidateView): Double {
    var total = 0.0
    var weights = 0.0

    fun add(weight: Double, value: Double) {
        total += weight * value
        weights += weight
    }

    if (query.title.full.isNotBlank()) {
        add(
            TITLE_WEIGHT,
            maxOf(
                textSimilarity(query.title.full, candidate.title.full),
                textSimilarity(query.title.core, candidate.title.core),
            ),
        )
    }
    if (query.artist != null) add(ARTIST_WEIGHT, artistScore(query, candidate))

    var albumMatch = 0.0
    if (query.album != null) {
        val album = candidate.candidate.album
        albumMatch =
            if (album == null) UNKNOWN_ALBUM
            else
                maxOf(
                    textSimilarity(query.album, album),
                    textSimilarity(query.albumCore.orEmpty(), candidate.albumCore.orEmpty()),
                )
        add(ALBUM_WEIGHT, albumMatch)
    }

    // Which performance: "Live From Radio City Music Hall" is not any live recording.
    if (query.detailWords.isNotEmpty()) {
        add(
            DETAIL_WEIGHT,
            query.detailWords.count { it in candidate.detailWords }.toDouble() /
                query.detailWords.size,
        )
    }

    var factor = variantFactor(query.variants, candidate.variants)

    val queryDuration = query.durationMs
    val candidateDuration = candidate.candidate.durationMs
    if (queryDuration != null && candidateDuration != null) {
        val seconds = abs(queryDuration - candidateDuration) / 1000.0
        // Within 2 s is the same recording; past 20 s it almost certainly is not.
        add(DURATION_WEIGHT, (1.0 - (seconds - 2.0).coerceAtLeast(0.0) / 18.0).coerceIn(0.0, 1.0))
        if (seconds > DIFFERENT_RECORDING_SECONDS) factor *= DIFFERENT_RECORDING_PENALTY
    }

    if (candidate.compilation && !query.compilation && albumMatch < SAME_ALBUM) {
        factor *= COMPILATION_PENALTY
    }

    if (weights == 0.0) return 0.0
    return total / weights * factor * query.trust
}

private fun artistScore(query: QueryReading, candidate: CandidateView): Double {
    val wanted = query.artist ?: return 0.0
    val credited = candidate.artists
    if (credited.isEmpty()) return 0.0

    fun best(name: String, among: List<String>) = among.maxOf { artistSimilarity(name, it) }

    val whole =
        maxOf(
            artistSimilarity(wanted, candidate.candidate.artists.joinToString(" ")),
            best(wanted, credited),
        )
    val primary = best(query.artists.first(), credited)
    val recall = query.artists.map { best(it, credited) }.average()
    val precision = credited.map { best(it, query.artists) }.average()
    val split = 0.65 * primary + 0.25 * recall + 0.1 * precision
    val albumArtist =
        candidate.candidate.albumArtists.maxOfOrNull { artistSimilarity(wanted, it) }?.times(0.9)
            ?: 0.0
    return maxOf(whole, split, albumArtist)
}

private fun variantFactor(wanted: Set<Variant>, offered: Set<Variant>): Double =
    (wanted - offered).fold(1.0) { acc, it -> acc * it.penalty } *
        (offered - wanted).fold(1.0) { acc, it -> acc * it.penalty }

/** For each candidate, how many other providers list the same recording. */
private fun corroboration(views: List<CandidateView>): List<Int> = views.map { view ->
    views
        .filter { other ->
            other.candidate.providerId != view.candidate.providerId && sameRecording(view, other)
        }
        .map { it.candidate.providerId }
        .distinct()
        .size
}

private fun sameRecording(a: CandidateView, b: CandidateView): Boolean {
    val isrcA = a.candidate.isrc
    val isrcB = b.candidate.isrc
    if (isrcA != null && isrcB != null) return isrcA.equals(isrcB, ignoreCase = true)
    if (a.coreKey != b.coreKey || a.recordingVariants != b.recordingVariants) return false
    val artistA = a.artists.firstOrNull() ?: return false
    val artistB = b.artists.firstOrNull() ?: return false
    if (artistSimilarity(artistA, artistB) < SAME_ARTIST) return false
    val durationA = a.candidate.durationMs ?: return true
    val durationB = b.candidate.durationMs ?: return true
    return abs(durationA - durationB) <= SAME_RECORDING_MS
}

private val TRACK_NUMBER = Regex("""^\s*\d{1,3}\s*[.\-_)]\s*""")

private const val TITLE_WEIGHT = 0.45
private const val ARTIST_WEIGHT = 0.3
private const val ALBUM_WEIGHT = 0.1
private const val DURATION_WEIGHT = 0.15
private const val DETAIL_WEIGHT = 0.1

/** A candidate without an album is neither the album asked for nor a different one. */
private const val UNKNOWN_ALBUM = 0.5
private const val SAME_ALBUM = 0.8
private const val COMPILATION_PENALTY = 0.92
private const val DIFFERENT_RECORDING_SECONDS = 30.0
private const val DIFFERENT_RECORDING_PENALTY = 0.9
private const val ARTIST_IN_TITLE_TRUST = 0.95

private const val CORROBORATION_BONUS = 0.03
private const val MAX_CORROBORATIONS = 2

/** Agreement lifts good matches; it does not rescue poor ones. */
private const val CORROBORATION_FLOOR = 0.5
private const val SAME_ARTIST = 0.9
private const val SAME_RECORDING_MS = 3_000L

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
