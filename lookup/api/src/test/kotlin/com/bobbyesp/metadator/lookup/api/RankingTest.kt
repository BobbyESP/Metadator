/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingTest {
    private fun candidate(
        id: String,
        title: String,
        artist: String,
        durationMs: Long? = null,
        album: String? = null,
        albumArtist: String? = null,
        provider: String = "test",
        isrc: String? = null,
        date: String? = null,
        artists: List<String> = listOf(artist),
    ) =
        LookupCandidate(
            providerId = provider,
            providerName = provider,
            id = id,
            title = title,
            artists = artists,
            album = album,
            albumArtists = listOfNotNull(albumArtist),
            durationMs = durationMs,
            isrc = isrc,
            date = date,
        )

    private fun ids(query: LookupQuery, vararg candidates: LookupCandidate) =
        rank(query, candidates.toList()).map { it.id }

    @Test
    fun `the closest title and artist rank first`() {
        assertEquals(
            listOf("2", "1", "3"),
            ids(
                LookupQuery(title = "Halo", artist = "Beyonce"),
                candidate("1", "Halo", "Haloo Helsinki"),
                candidate("2", "Halo", "Beyoncé"),
                candidate("3", "Hello", "Adele"),
            ),
        )
    }

    @Test
    fun `duration separates versions of the same song`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé", durationMs = 261_000),
                candidate("live", "Halo", "Beyoncé", 330_000),
                candidate("album", "Halo", "Beyoncé", 261_500),
            )
        assertEquals("album", ranked.first())
    }

    @Test
    fun `a version the query does not name loses to the plain recording`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                candidate("karaoke", "Halo (Karaoke Version)", "Beyoncé"),
                candidate("live", "Halo - Live at Wembley", "Beyoncé"),
                candidate("remix", "Halo (Dave Aude Remix)", "Beyoncé"),
                candidate("studio", "Halo", "Beyoncé"),
            )
        assertEquals("studio", ranked.first())
        assertEquals("karaoke", ranked.last())
    }

    @Test
    fun `a version the query names wins over the plain recording`() {
        assertEquals(
            "live",
            ids(
                    LookupQuery(title = "Halo (Live)", artist = "Beyoncé"),
                    candidate("studio", "Halo", "Beyoncé"),
                    candidate("live", "Halo - Live at Wembley", "Beyoncé"),
                )
                .first(),
        )
        assertEquals(
            "tv",
            ids(
                    LookupQuery(title = "Love Story (Taylor's Version)", artist = "Taylor Swift"),
                    candidate("original", "Love Story", "Taylor Swift"),
                    candidate("tv", "Love Story (Taylor's Version)", "Taylor Swift"),
                )
                .first(),
        )
    }

    @Test
    fun `decorations that do not change the recording keep a match high`() {
        val ranked =
            rank(
                LookupQuery(
                    title = "Bohemian Rhapsody",
                    artist = "Queen",
                    album = "A Night at the Opera",
                ),
                listOf(
                    candidate(
                        "1",
                        "Bohemian Rhapsody (Remastered 2011)",
                        "Queen",
                        album = "A Night At The Opera (Deluxe Edition 2011 Remaster)",
                    )
                ),
            )
        assertEquals(MatchConfidence.High, MatchConfidence.of(ranked.single().score))
    }

    @Test
    fun `karaoke albums and tribute artists are recognised as such`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                candidate("tribute", "Halo", "Beyoncé", album = "Made Famous by Beyoncé"),
                candidate("original", "Halo", "Beyoncé", album = "I Am... Sasha Fierce"),
            )
        assertEquals("original", ranked.first())
    }

    @Test
    fun `featured artists count wherever they are written`() {
        val ranked =
            rank(
                LookupQuery(title = "us.", artist = "Gracie Abrams feat. Taylor Swift"),
                listOf(
                    candidate("solo", "us.", "Gracie Abrams"),
                    candidate("featured", "us. (feat. Taylor Swift)", "Gracie Abrams"),
                ),
            )
        assertEquals("featured", ranked.first().id)
        assertEquals(MatchConfidence.High, MatchConfidence.of(ranked.first().score))
    }

    @Test
    fun `an artist written in the title is found there`() {
        val ranked =
            rank(
                LookupQuery(title = "Queen - Bohemian Rhapsody"),
                listOf(
                    candidate("cover", "Bohemian Rhapsody", "The Braids"),
                    candidate("queen", "Bohemian Rhapsody", "Queen"),
                ),
            )
        assertEquals("queen", ranked.first().id)
        assertTrue(ranked.first().score >= 0.85)
    }

    @Test
    fun `a track number before the title is ignored`() {
        val ranked =
            rank(
                LookupQuery(title = "01. Halo", artist = "Beyoncé"),
                listOf(candidate("1", "Halo", "Beyoncé")),
            )
        assertEquals(1.0, ranked.single().score, 0.0)
    }

    @Test
    fun `the original release beats a compilation`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                candidate(
                    "compilation",
                    "Halo",
                    "Beyoncé",
                    album = "Now That's What I Call Music! 73",
                    albumArtist = "Various Artists",
                ),
                candidate(
                    "album",
                    "Halo",
                    "Beyoncé",
                    album = "I Am... Sasha Fierce",
                    albumArtist = "Beyoncé",
                ),
            )
        assertEquals("album", ranked.first())
    }

    @Test
    fun `a compilation the query asks for is not penalised`() {
        val ranked =
            ids(
                LookupQuery(
                    title = "Halo",
                    artist = "Beyoncé",
                    album = "Now That's What I Call Music! 73",
                ),
                candidate("album", "Halo", "Beyoncé", album = "I Am... Sasha Fierce"),
                candidate(
                    "compilation",
                    "Halo",
                    "Beyoncé",
                    album = "Now That's What I Call Music! 73",
                    albumArtist = "Various Artists",
                ),
            )
        assertEquals("compilation", ranked.first())
    }

    @Test
    fun `a recording several providers agree on ranks first`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                candidate(
                    "other",
                    "Halo",
                    "Beyoncé",
                    300_000,
                    provider = "a",
                    isrc = "USSM10000001",
                ),
                candidate(
                    "agreed-a",
                    "Halo",
                    "Beyoncé",
                    261_000,
                    provider = "a",
                    isrc = "USSM10803364",
                ),
                candidate(
                    "agreed-b",
                    "Halo",
                    "Beyoncé",
                    261_000,
                    provider = "b",
                    isrc = "ussm10803364",
                ),
            )
        assertEquals(setOf("agreed-a", "agreed-b"), ranked.take(2).toSet())
    }

    @Test
    fun `equal matches go to the one with more metadata, then the earliest`() {
        val ranked =
            ids(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                candidate("bare", "Halo", "Beyoncé"),
                candidate("later", "Halo", "Beyoncé", album = "Halo", date = "2011"),
                candidate(
                    "earlier",
                    "Halo",
                    "Beyoncé",
                    album = "I Am... Sasha Fierce",
                    date = "2008",
                ),
            )
        assertEquals(listOf("earlier", "later", "bare"), ranked)
    }

    @Test
    fun `word order and articles do not matter for artists`() {
        val ranked =
            rank(
                LookupQuery(title = "Help!", artist = "Beatles"),
                listOf(candidate("1", "Help!", "The Beatles")),
            )
        assertEquals(1.0, ranked.single().score, 0.0)
    }

    @Test
    fun `scores stay between 0 and 1`() {
        val ranked =
            rank(
                LookupQuery(title = "Halo", artist = "Beyoncé"),
                listOf(
                    candidate("a", "Halo", "Beyoncé", provider = "a", isrc = "X"),
                    candidate("b", "Halo", "Beyoncé", provider = "b", isrc = "X"),
                    candidate("c", "Halo", "Beyoncé", provider = "c", isrc = "X"),
                ),
            )
        assertTrue(ranked.all { it.score in 0.0..1.0 })
    }

    @Test
    fun `confidence follows the score`() {
        assertEquals(MatchConfidence.High, MatchConfidence.of(0.9))
        assertEquals(MatchConfidence.Medium, MatchConfidence.of(0.7))
        assertEquals(MatchConfidence.Low, MatchConfidence.of(0.2))
    }
}
