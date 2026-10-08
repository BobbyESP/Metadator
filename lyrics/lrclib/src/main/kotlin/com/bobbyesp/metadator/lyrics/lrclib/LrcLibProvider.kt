/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.lrclib

import com.bobbyesp.metadator.core.network.isOfflineError
import com.bobbyesp.metadator.core.network.networkCall
import com.bobbyesp.metadator.lyrics.api.Lyrics
import com.bobbyesp.metadator.lyrics.api.LyricsProvider
import com.bobbyesp.metadator.lyrics.api.LyricsQuery
import com.bobbyesp.metadator.lyrics.api.LyricsResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * LRCLIB, an open database of synced lyrics. An exact match (title, artist, album, duration) comes
 * first; when that misses, a search, keeping the result closest in duration.
 */
class LrcLibProvider(private val client: HttpClient) : LyricsProvider {

    override val displayName: String = "LRCLIB"

    override suspend fun find(query: LyricsQuery): LyricsResult =
        networkCall(
            onError = {
                if (isOfflineError(it)) LyricsResult.Offline
                else LyricsResult.Failed(it.message ?: it::class.simpleName.orEmpty())
            }
        ) {
            exactMatch(query)?.let {
                return@networkCall LyricsResult.Found(it)
            }
            searchMatch(query)?.let { LyricsResult.Found(it) } ?: LyricsResult.NotFound
        }

    private suspend fun exactMatch(query: LyricsQuery): Lyrics? {
        val response =
            client.get("$BASE_URL/get") {
                parameter("track_name", query.title)
                parameter("artist_name", query.artist)
                query.album?.takeIf { it.isNotBlank() }?.let { parameter("album_name", it) }
                query.durationMs?.let { parameter("duration", it / 1000) }
            }
        if (response.status == HttpStatusCode.NotFound || !response.status.isSuccess()) return null
        return response.body<LrcLibRecord>().toLyrics()
    }

    private suspend fun searchMatch(query: LyricsQuery): Lyrics? {
        val response =
            client.get("$BASE_URL/search") {
                parameter("track_name", query.title)
                parameter("artist_name", query.artist)
            }
        if (!response.status.isSuccess()) return null
        val records = response.body<List<LrcLibRecord>>().filter { it.toLyrics() != null }
        val target = query.durationMs
        val best =
            if (target == null) records.firstOrNull()
            else
                records
                    .mapNotNull { record ->
                        record.duration?.let { record to abs(it * 1000 - target) }
                    }
                    .minByOrNull { (_, gap) -> gap }
                    ?.takeIf { (_, gap) -> gap <= MAX_DURATION_GAP_MS }
                    ?.first ?: records.firstOrNull { it.duration == null }
        return best?.toLyrics()
    }

    private companion object {
        const val BASE_URL = "https://lrclib.net/api"
        const val MAX_DURATION_GAP_MS = 5_000
    }
}

@Serializable
internal data class LrcLibRecord(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
) {
    fun toLyrics(): Lyrics? =
        if (plainLyrics.isNullOrBlank() && syncedLyrics.isNullOrBlank() && !instrumental) null
        else Lyrics(plain = plainLyrics, synced = syncedLyrics, instrumental = instrumental)
}
