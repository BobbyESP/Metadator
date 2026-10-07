/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.deezer

import com.bobbyesp.metadator.core.network.HttpClientFactory
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.lookup.api.LookupResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeezerProviderTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `search results are completed with track details`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/search" ->
                    respond(
                        """{"data":[{"id":7,"title":"Halo","duration":261,"artist":{"name":"Beyoncé"},
                           "album":{"id":1,"title":"I Am... Sasha Fierce","cover_xl":"xl","cover_medium":"md"}}]}""",
                        HttpStatusCode.OK,
                        json,
                    )
                "/track/7" ->
                    respond(
                        """{"id":7,"title":"Halo","duration":261,"isrc":"USSM10804556","track_position":2,
                           "disk_number":1,"release_date":"2008-11-14","artist":{"name":"Beyoncé"},
                           "contributors":[{"name":"Beyoncé","role":"Main"}],
                           "album":{"id":1,"title":"I Am... Sasha Fierce"}}""",
                        HttpStatusCode.OK,
                        json,
                    )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val provider = DeezerProvider(HttpClientFactory.create("test", engine))

        val result = provider.search(LookupQuery("Halo", "Beyoncé")) as LookupResult.Success
        val halo = result.candidates.single()

        assertEquals(2, halo.trackNumber)
        assertEquals("USSM10804556", halo.isrc)
        assertEquals("2008-11-14", halo.date)
        assertEquals("xl", halo.artworkUrl)
        assertEquals(261_000L, halo.durationMs)
    }

    @Test
    fun `quota errors are rate limiting`() = runTest {
        val engine = MockEngine {
            respond(
                """{"error":{"type":"Exception","message":"Quota limit exceeded","code":4}}""",
                HttpStatusCode.OK,
                json,
            )
        }
        assertEquals(
            LookupResult.RateLimited,
            DeezerProvider(HttpClientFactory.create("t", engine)).search(LookupQuery("x")),
        )
    }
}
