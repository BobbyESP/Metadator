/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.lrclib

import com.bobbyesp.metadator.core.network.HttpClientFactory
import com.bobbyesp.metadator.lyrics.api.LyricsQuery
import com.bobbyesp.metadator.lyrics.api.LyricsResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcLibProviderTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `falls back to search and keeps the closest duration`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/get" ->
                    respond(
                        """{"code":404,"name":"TrackNotFound"}""",
                        HttpStatusCode.NotFound,
                        json,
                    )
                "/api/search" ->
                    respond(
                        """[{"id":1,"duration":300.0,"plainLyrics":"far"},
                            {"id":2,"duration":261.0,"plainLyrics":"close","syncedLyrics":"[00:01.00]close"}]""",
                        HttpStatusCode.OK,
                        json,
                    )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val result =
            LrcLibProvider(HttpClientFactory.create("t", engine))
                .find(LyricsQuery("Halo", "Beyoncé", durationMs = 262_000)) as LyricsResult.Found

        assertEquals("[00:01.00]close", result.lyrics.preferred)
    }

    @Test
    fun `nothing found is not an error`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/api/search") respond("[]", HttpStatusCode.OK, json)
            else respond("{}", HttpStatusCode.NotFound, json)
        }
        assertEquals(
            LyricsResult.NotFound,
            LrcLibProvider(HttpClientFactory.create("t", engine)).find(LyricsQuery("x", "y")),
        )
    }
}
