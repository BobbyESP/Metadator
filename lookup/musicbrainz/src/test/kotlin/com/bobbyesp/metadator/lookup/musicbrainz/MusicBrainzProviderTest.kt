/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.musicbrainz

import com.bobbyesp.metadator.core.network.HttpClientFactory
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.lookup.api.LookupResult
import com.bobbyesp.metadator.lookup.api.MatchConfidence
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
    fun `a release with the recording on several media gives one candidate`() = runTest {
        val landspace =
            """
            {"recordings":[{"id":"ab7be9dd","title":"crossing field (CM SPOT)",
              "artist-credit":[{"name":"LiSA"}],
              "releases":[
                {"id":"b5fe1a0e","title":"LANDSPACE","status":"Official",
                 "media":[{"position":3,"format":"DVD-Video","track":[{"id":"t-dvd","number":"7"}]}]},
                {"id":"b5fe1a0e","title":"LANDSPACE","status":"Official",
                 "media":[{"position":2,"format":"Blu-ray","track":[{"id":"t-bd","number":"7"}]}]},
                {"id":"other","title":"Other","status":"Official",
                 "media":[{"position":1,"track":[{"id":"t-o","number":"1"}]}]}
              ]}]}
            """
                .trimIndent()
        val engine = MockEngine {
            respond(
                landspace,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val provider = MusicBrainzProvider(HttpClientFactory.create("test", engine))

        val result =
            provider.search(LookupQuery("crossing field (CM SPOT)", "LiSA")) as LookupResult.Success

        assertEquals(
            listOf("ab7be9dd/b5fe1a0e", "ab7be9dd/other"),
            result.candidates.map { it.id }.sorted(),
        )
        val landspaceCandidate = result.candidates.first { it.album == "LANDSPACE" }
        assertEquals(2, landspaceCandidate.discNumber)
    }

    private suspend fun searchFixture(name: String, query: LookupQuery): List<LookupCandidate> {
        val fixture = javaClass.getResource("/$name.json")!!.readText()
        val engine = MockEngine {
            respond(
                fixture,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val provider = MusicBrainzProvider(HttpClientFactory.create("test", engine))
        return (provider.search(query) as LookupResult.Success).candidates
    }

    // Fixtures are real MusicBrainz answers (October 2026), trimmed to the fields the app reads.

    @Test
    fun `the studio recording ranks above live, video and DJ-mix ones with the same title`() =
        runTest {
            val ranked =
                searchFixture(
                    "gracie-abrams-thats-so-true",
                    LookupQuery("That's So True", "Gracie Abrams"),
                )

            assertEquals("explicit", ranked.first().version)
            assertEquals(listOf("Album"), ranked.first().releaseTypes)
            val studio = ranked.indexOfLast {
                it.version == "explicit" && "Compilation" !in it.releaseTypes
            }
            val firstLive = ranked.indexOfFirst {
                it.version.orEmpty().contains("live", ignoreCase = true)
            }
            assertTrue(studio < firstLive)
            assertTrue(
                ranked.filter { it.version.orEmpty().startsWith("live") }.all { it.score < 0.85 }
            )
            val djMix = ranked.first { it.album == "2025 MegaMix" }
            assertEquals(listOf("Album", "DJ-mix"), djMix.releaseTypes)
            assertTrue(djMix.score < 0.85)
        }

    @Test
    fun `a live query finds that performance first`() = runTest {
        val ranked =
            searchFixture(
                "gracie-abrams-thats-so-true",
                LookupQuery("That's So True (Live From Radio City Music Hall)", "Gracie Abrams"),
            )

        val radioCity = ranked.indexOfLast { it.version.orEmpty().contains("Radio City") }
        val redRocks = ranked.indexOfFirst { it.version.orEmpty().contains("Red Rocks") }
        assertTrue(ranked.first().version.orEmpty().contains("Radio City"))
        assertTrue(radioCity < redRocks)
        assertTrue(ranked.take(redRocks).none { it.version == "explicit" })
    }

    @Test
    fun `a live recording without a duration is still told apart`() = runTest {
        val ranked =
            searchFixture(
                "gracie-abrams-i-love-you-im-sorry",
                LookupQuery("I Love You, I'm Sorry", "Gracie Abrams", durationMs = 157_000),
            )

        assertEquals("explicit", ranked.first().version)
        val fireAid = ranked.first { it.album.orEmpty().contains("FireAid") }
        assertEquals(null, fireAid.durationMs)
        assertTrue(MatchConfidence.of(fireAid.score) != MatchConfidence.High)
        assertTrue(ranked.filter { it.version == "explicit" }.all { it.score > fireAid.score })
    }

    @Test
    fun `one release of each name per recording`() = runTest {
        val ranked =
            searchFixture(
                "gracie-abrams-thats-so-true",
                LookupQuery("That's So True", "Gracie Abrams"),
            )

        val perRecording = ranked.groupBy { it.id.substringBefore('/') }
        assertTrue(
            perRecording.values.all { releases ->
                releases.map { it.album }.distinct().size == releases.size
            }
        )
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
