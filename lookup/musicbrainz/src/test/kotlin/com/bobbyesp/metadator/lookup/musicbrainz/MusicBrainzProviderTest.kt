/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.musicbrainz

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
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBrainzProviderTest {
    private val body =
        """
        {"recordings":[{"id":"rec-1","score":100,"title":"Bohemian Rhapsody","length":354000,
          "first-release-date":"1975-10-31",
          "artist-credit":[{"name":"Queen","joinphrase":"","artist":{"id":"art-1","name":"Queen"}}],
          "releases":[
            {"id":"rel-comp","title":"Greatest Hits","status":"Official","date":"1981",
             "release-group":{"id":"rg-2","primary-type":"Album","secondary-types":["Compilation"]},
             "media":[{"position":1,"track-count":17,"track":[{"id":"t-2","number":"1"}]}]},
            {"id":"rel-1","title":"A Night at the Opera","status":"Official","date":"1975-11-21",
             "release-group":{"id":"rg-1","primary-type":"Album"},
             "media":[{"position":1,"format":"CD","track-count":12,"track":[{"id":"t-1","number":"11"}]}]}
          ],
          "tags":[{"count":5,"name":"progressive rock"}]}]}
        """
            .trimIndent()

    @Test
    fun `parses recordings into one candidate per release, studio album first`() = runTest {
        var requested = ""
        val engine = MockEngine { request ->
            requested = request.url.parameters["query"].orEmpty()
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val provider = MusicBrainzProvider(HttpClientFactory.create("test", engine))

        val result =
            provider.search(LookupQuery("Bohemian Rhapsody", "Queen", durationMs = 354_000))
                as LookupResult.Success

        assertEquals("recording:\"Bohemian Rhapsody\" AND artist:\"Queen\"", requested)
        assertEquals(2, result.candidates.size)
        val best = result.candidates.first { it.album == "A Night at the Opera" }
        assertEquals(11, best.trackNumber)
        assertEquals(12, best.trackTotal)
        assertEquals("1975-11-21", best.date)
        assertEquals(listOf("Progressive Rock"), best.genres)
        assertEquals("https://coverartarchive.org/release-group/rg-1/front-1200", best.artworkUrl)
        assertEquals("rec-1", best.identifiers["MUSICBRAINZ_TRACKID"])
        assertTrue(result.candidates.all { it.score > 0.9 })
    }

    @Test
    fun `quotes in queries are escaped`() {
        assertEquals(
            "recording:\"Say \\\"Hi\\\"\"",
            MusicBrainzProvider.buildLuceneQuery(LookupQuery("Say \"Hi\"")),
        )
    }

    @Test
    fun `503 means rate limited`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }
        val provider = MusicBrainzProvider(HttpClientFactory.create("test", engine))
        assertEquals(LookupResult.RateLimited, provider.search(LookupQuery("x")))
    }
}
