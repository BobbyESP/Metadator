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
 * Deezer's public catalogue: no key, broad coverage of commercial releases and covers up to
 * 1000 px. Search results are thin, so the best few are completed with their track details.
 */
class DeezerProvider(private val client: HttpClient) : MetadataProvider {

    override val id: String = ID
    override val displayName: String = "Deezer"
    override val providesArtwork: Boolean = true

    override suspend fun search(query: LookupQuery): LookupResult {
        if (query.isBlank) return LookupResult.Success(emptyList())
        return networkCall(
            onError = {
                if (isOfflineError(it)) LookupResult.Offline
                else LookupResult.Failed(it.message ?: it::class.simpleName.orEmpty())
            }
        ) {
            val response =
                client.get("$BASE_URL/search") {
                    parameter("q", buildQuery(query))
                    parameter("limit", RESULT_LIMIT)
                }
            if (!response.status.isSuccess()) {
                return@networkCall LookupResult.Failed("HTTP ${response.status.value}")
            }
            val search = response.body<DeezerSearch>()
            search.error?.let {
                return@networkCall if (it.code == QUOTA_EXCEEDED) LookupResult.RateLimited
                else LookupResult.Failed(it.message ?: "Deezer error ${it.code}")
            }

            val ranked = rank(query, search.data.map { it.toCandidate() })
            val detailed = coroutineScope {
                ranked.take(DETAILED_RESULTS).map { async { details(it) ?: it } }.awaitAll()
            }
            LookupResult.Success(rank(query, detailed + ranked.drop(DETAILED_RESULTS)))
        }
    }

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
        const val ID = "deezer"
        private const val BASE_URL = "https://api.deezer.com"
        private const val RESULT_LIMIT = 15
        private const val DETAILED_RESULTS = 5
        private const val QUOTA_EXCEEDED = 4

        internal fun buildQuery(query: LookupQuery): String =
            buildList {
                    add("track:${quote(query.title)}")
                    query.artist?.takeIf { it.isNotBlank() }?.let { add("artist:${quote(it)}") }
                    query.album?.takeIf { it.isNotBlank() }?.let { add("album:${quote(it)}") }
                }
                .joinToString(" ")

        private fun quote(value: String) = "\"" + value.replace("\"", "") + "\""

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
