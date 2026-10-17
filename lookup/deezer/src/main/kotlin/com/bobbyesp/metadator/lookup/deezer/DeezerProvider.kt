/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.deezer

import com.bobbyesp.metadator.core.network.isOfflineError
import com.bobbyesp.metadator.core.network.networkCall
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.lookup.api.LookupResult
import com.bobbyesp.metadator.lookup.api.MetadataProvider
import com.bobbyesp.metadator.lookup.api.rank
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * [MetadataProvider] implementation backed by the Deezer public API.
 *
 * This provider performs concurrent search requests using different query combinations, ranks the
 * returned tracks, and optionally enriches the most relevant results with track details to improve
 * metadata quality and artwork coverage.
 */
class DeezerProvider(private val client: HttpClient) : MetadataProvider {

    /** Unique provider identifier used by the lookup system. */
    override val id: String = ID

    /** Human-readable provider name exposed to the UI. */
    override val displayName: String = "Deezer"

    /** Deezer search results can include album artwork URLs. */
    override val providesArtwork: Boolean = true

    /**
     * Searches Deezer for the given query and returns ranked metadata candidates.
     *
     * The search strategy tries several text variants in parallel, merges the results, and falls
     * back to a title-only lookup when a stricter artist/title/album search produces no matches.
     */
    override suspend fun search(query: LookupQuery): LookupResult {
        if (query.isBlank) return LookupResult.Success(emptyList())
        return networkCall(
            onError = {
                if (isOfflineError(it)) LookupResult.Offline
                else LookupResult.Failed(it.message ?: it::class.simpleName.orEmpty())
            }
        ) {
            val searches = coroutineScope {
                searchTexts(query).map { async { searchTracks(it) } }.awaitAll()
            }
            searches
                .firstNotNullOfOrNull { it?.error }
                ?.let {
                    return@networkCall it.toResult()
                }
            if (searches.all { it == null }) {
                return@networkCall LookupResult.Failed("Deezer search failed")
            }
            var tracks = searches.filterNotNull().flatMap { it.data }.distinctBy { it.id }
            // Every word must match, so one the catalogue lacks ("feat. …") empties the search.
            if (tracks.isEmpty() && !query.artist.isNullOrBlank()) {
                val byTitle =
                    searchTracks(query.title.trim())
                        ?: return@networkCall LookupResult.Failed("Deezer search failed")
                byTitle.error?.let {
                    return@networkCall it.toResult()
                }
                tracks = byTitle.data
            }

            val ranked = rank(query, tracks.map { it.toCandidate() })
            val detailed = coroutineScope {
                ranked.take(DETAILED_RESULTS).map { async { details(it) ?: it } }.awaitAll()
            }
            LookupResult.Success(rank(query, detailed + ranked.drop(DETAILED_RESULTS)))
        }
    }

    /** Performs a Deezer track search for the provided free-text query. */
    private suspend fun searchTracks(text: String): DeezerSearch? {
        if (text.isBlank()) return DeezerSearch()
        val response =
            client.get("$BASE_URL/search") {
                parameter("q", text)
                parameter("limit", RESULT_LIMIT)
            }
        return if (response.status.isSuccess()) response.body<DeezerSearch>() else null
    }

    /**
     * Fetches a track's full details and merges them with the original search hit.
     *
     * Deezer's detail endpoint may omit artwork information that was already present in the search
     * response, so the original artwork fields are preserved when needed.
     */
    private suspend fun details(candidate: LookupCandidate): LookupCandidate? =
        networkCall(onError = { null }) {
            val response = client.get("$BASE_URL/track/${candidate.id}")
            if (!response.status.isSuccess()) return@networkCall null
            val track = response.body<DeezerTrack>()
            if (track.error != null) return@networkCall null
            val detailed = track.toCandidate()
            detailed.copy(
                // The search hit carries the album's covers even when the detail call does not.
                artworkUrl = detailed.artworkUrl ?: candidate.artworkUrl,
                artworkThumbnailUrl = detailed.artworkThumbnailUrl ?: candidate.artworkThumbnailUrl,
            )
        }

    companion object {
        /** Stable identifier for Deezer lookups. */
        const val ID = "deezer"

        /** Base Deezer REST API endpoint. */
        private const val BASE_URL = "https://api.deezer.com"

        /** Maximum number of results requested from Deezer for each search variant. */
        private const val RESULT_LIMIT = 15

        /** Number of top-ranked results that should be enriched with detailed track data. */
        private const val DETAILED_RESULTS = 5

        /** Deezer API error code returned when the quota or rate limit is exceeded. */
        private const val QUOTA_EXCEEDED = 4

        /**
         * Builds the ordered list of text queries used to search Deezer.
         *
         * The provider first tries a strict title/artist/album combination and then a slightly
         * looser title/artist query. Empty variants are removed and duplicates are collapsed to
         * avoid redundant requests.
         */
        internal fun searchTexts(query: LookupQuery): List<String> =
            listOf(
                    words(query.title, query.artist, query.album),
                    words(query.title, query.artist),
                )
                .filter { it.isNotEmpty() }
                .distinct()

        /** Joins non-blank string parts into a single space-separated search phrase. */
        private fun words(vararg parts: String?) =
            parts
                .filterNotNull()
                .map { it.replace("\"", " ").trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")

        /** Maps a Deezer API error into a lookup result understood by the app. */
        private fun DeezerError.toResult(): LookupResult =
            if (code == QUOTA_EXCEEDED) LookupResult.RateLimited
            else LookupResult.Failed(message ?: "Deezer error $code")

        /** Converts a Deezer track payload into the app's normalized lookup candidate model. */
        internal fun DeezerTrack.toCandidate(): LookupCandidate {
            val mainArtists =
                contributors.filter { it.role == null || it.role == "Main" }.map { it.name }
            return LookupCandidate(
                providerId = ID,
                providerName = "Deezer",
                id = id.toString(),
                title = title,
                artists = mainArtists.ifEmpty { listOfNotNull(artist?.name) },
                album = album?.title,
                albumArtists = listOfNotNull(artist?.name),
                date = releaseDate ?: album?.releaseDate,
                trackNumber = trackPosition,
                discNumber = diskNumber,
                isrc = isrc,
                durationMs = duration?.times(1000),
                artworkUrl = album?.coverXl,
                artworkThumbnailUrl = album?.coverMedium,
            )
        }
    }
}
