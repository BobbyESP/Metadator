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
        version: String? = null,
        releaseTypes: List<String> = emptyList(),
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
            version = version,
            releaseTypes = releaseTypes,
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
    fun `the provider's version tells recordings with the same title apart`() {
        val ranked =
            ids(
                LookupQuery(title = "Risk", artist = "Gracie Abrams"),
                candidate(
                    "live",
                    "Risk",
                    "Gracie Abrams",
                    version = "live, 2024-10-04, Radio City Music Hall",
                ),
                candidate("video", "Risk", "Gracie Abrams", version = "music video"),
                candidate("atmos", "Risk", "Gracie Abrams", version = "Dolby Atmos mix"),
                candidate("studio", "Risk", "Gracie Abrams"),
            )
        assertEquals(listOf("studio", "atmos", "video", "live"), ranked)
    }

    @Test
    fun `a Dolby Atmos mix is not a remix`() {
        val atmos =
            score(
                LookupQuery("Risk", "Gracie Abrams"),
                candidate("1", "Risk", "Gracie Abrams", version = "Dolby Atmos mix"),
            )
        assertTrue(atmos > 0.9)
    }

    @Test
    fun `release types count, but a recording that describes itself wins`() {
        val query = LookupQuery("Risk", "Gracie Abrams")
        val interview =
            candidate("1", "Risk", "Gracie Abrams", releaseTypes = listOf("Other", "Spokenword"))
        val onLiveSingle =
            candidate(
                "2",
                "Risk",
                "Gracie Abrams",
                version = "official lyric video",
                releaseTypes = listOf("Single", "Live"),
            )
        val undescribedLive =
            candidate("3", "Risk", "Gracie Abrams", releaseTypes = listOf("Album", "Live"))
        val compilation =
            candidate(
                "4",
                "Risk",
                "Gracie Abrams",
                album = "Cozy",
                releaseTypes = listOf("Album", "Compilation"),
            )

        assertTrue(score(query, interview) < 0.5)
        assertEquals(0.9, score(query, onLiveSingle), 0.001)
        assertEquals(0.75, score(query, undescribedLive), 0.001)
        assertEquals(0.92, score(query, compilation), 0.001)
    }

    @Test
    fun `the details of a version pick the right performance`() {
        val ranked =
            ids(
                LookupQuery("That's So True (Live From Radio City Music Hall)", "Gracie Abrams"),
                candidate(
                    "snl",
                    "That's So True",
                    "Gracie Abrams",
                    version = "live: 2024-12-14, Saturday Night Live",
                ),
                candidate(
                    "red-rocks",
                    "That's So True (Apple Music Live)",
                    "Gracie Abrams",
                    album = "Apple Music Live: Gracie Abrams",
                ),
                candidate(
                    "radio-city",
                    "That's So True",
                    "Gracie Abrams",
                    version = "live, 2024-10: Radio City Music Hall, New York, USA",
                ),
            )
        assertEquals("radio-city", ranked.first())
    }

    @Test
    fun `covers credited in the title or the album are recognised`() {
        val query = LookupQuery("That's So True", "Gracie Abrams")
        listOf(
                candidate(
                    "1",
                    "That's So True (Originally by Gracie Abrams) (Instrumental)",
                    "StudiOke",
                ),
                candidate(
                    "2",
                    "That’s So True (By Gracie Abrams) (Instrumental Karaoke Version)",
                    "ZZang KARAOKE",
                ),
                candidate(
                    "3",
                    "That's So True (Lower Key) [Originally Performed by Gracie Abrams] (Karaoke Version)",
                    "karaoke SESH",
                ),
                candidate(
                    "4",
                    "That's So True",
                    "Piano Dreamers",
                    album = "Piano Dreamers Perform Gracie Abrams (Instrumental)",
                ),
                candidate(
                    "5",
                    "That's So True",
                    "Acoustic Heartstrings",
                    album = "Acoustic Guitar Renditions of Gracie Abrams",
                ),
            )
            .forEach {
                assertEquals(it.title, MatchConfidence.Low, MatchConfidence.of(score(query, it)))
            }
    }

    @Test
    fun `confidence follows the score`() {
        assertEquals(MatchConfidence.High, MatchConfidence.of(0.9))
        assertEquals(MatchConfidence.Medium, MatchConfidence.of(0.7))
        assertEquals(MatchConfidence.Low, MatchConfidence.of(0.2))
    }
}
