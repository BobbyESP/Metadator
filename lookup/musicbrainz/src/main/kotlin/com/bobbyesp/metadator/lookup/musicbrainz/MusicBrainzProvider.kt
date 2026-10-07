package com.bobbyesp.metadator.lookup.musicbrainz

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
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MusicBrainz, the open music encyclopedia, with covers from the Cover Art Archive. No key is
 * needed, which is what lets the FOSS build ship it, but the service allows one request per
 * second per client and blocks clients that do more.
 */
class MusicBrainzProvider(
    private val client: HttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
) : MetadataProvider {

    override val id: String = ID
    override val displayName: String = "MusicBrainz"
    override val providesArtwork: Boolean = true

    private val rateLimit = Mutex()
    private var lastRequestAt = 0L

    override suspend fun search(query: LookupQuery): LookupResult {
        if (query.isBlank) return LookupResult.Success(emptyList())
        return networkCall(
            onError = {
                if (isOfflineError(it)) LookupResult.Offline
                else LookupResult.Failed(it.message ?: it::class.simpleName.orEmpty())
            }
        ) {
            val response =
                throttled {
                    client.get("$BASE_URL/recording") {
                        parameter("query", buildLuceneQuery(query))
                        parameter("fmt", "json")
                        parameter("limit", RESULT_LIMIT)
                    }
                }
            when {
                response.status == HttpStatusCode.ServiceUnavailable ||
                    response.status == HttpStatusCode.TooManyRequests -> LookupResult.RateLimited
                !response.status.isSuccess() -> LookupResult.Failed("HTTP ${response.status.value}")
                else -> {
                    val recordings = response.body<RecordingSearch>().recordings
                    LookupResult.Success(rank(query, recordings.flatMap { it.toCandidates() }).take(MAX_CANDIDATES))
                }
            }
        }
    }

    private suspend fun <T> throttled(block: suspend () -> T): T =
        rateLimit.withLock {
            val wait = lastRequestAt + MIN_INTERVAL_MS - clock()
            if (wait > 0) delay(wait)
            try {
                block()
            } finally {
                lastRequestAt = clock()
            }
        }

    companion object {
        const val ID = "musicbrainz"
        private const val BASE_URL = "https://musicbrainz.org/ws/2"
        private const val COVER_ART_URL = "https://coverartarchive.org"
        private const val RESULT_LIMIT = 15
        private const val MAX_CANDIDATES = 20
        private const val MIN_INTERVAL_MS = 1_100L

        /** At most this many releases per recording: the rest are reissues of the same thing. */
        private const val RELEASES_PER_RECORDING = 3

        internal fun buildLuceneQuery(query: LookupQuery): String =
            buildList {
                    add("recording:${quote(query.title)}")
                    query.artist?.takeIf { it.isNotBlank() }?.let { add("artist:${quote(it)}") }
                    query.album?.takeIf { it.isNotBlank() }?.let { add("release:${quote(it)}") }
                }
                .joinToString(" AND ")

        /** Quotes a phrase for Lucene, escaping what would end it early. */
        private fun quote(value: String): String =
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

        internal fun Recording.toCandidates(): List<LookupCandidate> {
            val artists = artistCredit.map { it.name }
            val genres = tags.sortedByDescending { it.count }.take(3).map { it.name.toTitleCase() }
            val releases =
                releases
                    .sortedWith(
                        compareByDescending<Release> { it.status == "Official" }
                            .thenByDescending { it.releaseGroup?.primaryType == "Album" }
                            .thenBy { it.releaseGroup?.secondaryTypes?.size ?: 0 }
                            .thenBy { it.date ?: "9999" }
                    )
                    .take(RELEASES_PER_RECORDING)

            if (releases.isEmpty()) {
                return listOf(
                    LookupCandidate(
                        providerId = ID,
                        providerName = "MusicBrainz",
                        id = id,
                        title = title,
                        artists = artists,
                        date = firstReleaseDate,
                        genres = genres,
                        isrc = isrcs.firstOrNull(),
                        durationMs = length,
                        identifiers = mapOf("MUSICBRAINZ_TRACKID" to id),
                    )
                )
            }

            return releases.map { release ->
                val medium = release.media.firstOrNull()
                val track = medium?.track?.firstOrNull()
                val releaseGroupId = release.releaseGroup?.id
                LookupCandidate(
                    providerId = ID,
                    providerName = "MusicBrainz",
                    id = "$id/${release.id}",
                    title = title,
                    artists = artists,
                    album = release.title,
                    albumArtists = release.artistCredit.map { it.name }.ifEmpty { artists },
                    date = release.date ?: firstReleaseDate,
                    trackNumber = track?.number?.toIntOrNull() ?: medium?.trackOffset?.plus(1),
                    trackTotal = medium?.trackCount ?: release.trackCount,
                    discNumber = medium?.position,
                    discTotal = null,
                    genres = genres,
                    isrc = isrcs.firstOrNull(),
                    durationMs = length ?: track?.length,
                    // The release group's cover is the one most likely to exist: a group picks
                    // one of its releases' covers even when this release has none.
                    artworkUrl =
                        releaseGroupId?.let { "$COVER_ART_URL/release-group/$it/front-1200" }
                            ?: "$COVER_ART_URL/release/${release.id}/front-1200",
                    artworkThumbnailUrl =
                        releaseGroupId?.let { "$COVER_ART_URL/release-group/$it/front-250" }
                            ?: "$COVER_ART_URL/release/${release.id}/front-250",
                    identifiers =
                        buildMap {
                            put("MUSICBRAINZ_TRACKID", id)
                            put("MUSICBRAINZ_ALBUMID", release.id)
                            releaseGroupId?.let { put("MUSICBRAINZ_RELEASEGROUPID", it) }
                            artistCredit.firstOrNull()?.artist?.id?.let {
                                put("MUSICBRAINZ_ARTISTID", it)
                            }
                            track?.id?.let { put("MUSICBRAINZ_RELEASETRACKID", it) }
                        },
                )
            }
        }

        private fun String.toTitleCase(): String =
            split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
    }
}
