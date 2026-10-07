package com.bobbyesp.metadator.lookup.api

import org.junit.Assert.assertEquals
import org.junit.Test

class RankingTest {
    private fun candidate(id: String, title: String, artist: String, durationMs: Long? = null) =
        LookupCandidate(
            providerId = "test",
            providerName = "Test",
            id = id,
            title = title,
            artists = listOf(artist),
            durationMs = durationMs,
        )

    @Test
    fun `the closest title and artist rank first`() {
        val query = LookupQuery(title = "Halo", artist = "Beyonce")
        val ranked =
            rank(
                query,
                listOf(
                    candidate("1", "Halo", "Haloo Helsinki"),
                    candidate("2", "Halo", "Beyoncé"),
                    candidate("3", "Hello", "Adele"),
                ),
            )
        assertEquals(listOf("2", "1", "3"), ranked.map { it.id })
    }

    @Test
    fun `duration separates versions of the same song`() {
        val query = LookupQuery(title = "Halo", artist = "Beyoncé", durationMs = 261_000)
        val ranked =
            rank(
                query,
                listOf(
                    candidate("live", "Halo", "Beyoncé", 330_000),
                    candidate("album", "Halo", "Beyoncé", 261_500),
                ),
            )
        assertEquals("album", ranked.first().id)
    }

    @Test
    fun `confidence follows the score`() {
        assertEquals(MatchConfidence.High, MatchConfidence.of(0.9))
        assertEquals(MatchConfidence.Medium, MatchConfidence.of(0.7))
        assertEquals(MatchConfidence.Low, MatchConfidence.of(0.2))
    }
}
