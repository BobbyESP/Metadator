package com.bobbyesp.metadator.core.domain.lookup

import com.bobbyesp.metadator.core.domain.FakeSettings
import com.bobbyesp.metadator.core.domain.editor.PositionStyle
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.lookup.api.LookupResult
import com.bobbyesp.metadator.lookup.api.MetadataProvider
import com.bobbyesp.metadator.tags.api.TagMap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupTest {
    private fun candidate(provider: String, title: String) =
        LookupCandidate(provider, provider, title, title, listOf("Queen"))

    private class Provider(override val id: String, val result: LookupResult) : MetadataProvider {
        override val displayName = id
        override val providesArtwork = true

        override suspend fun search(query: LookupQuery) = result
    }

    @Test
    fun `results from every provider are merged, failures named`() = runTest {
        val service =
            LookupService(
                listOf(
                    Provider("musicbrainz", LookupResult.Success(listOf(candidate("musicbrainz", "Bohemian Rhapsody")))),
                    Provider("deezer", LookupResult.Failed("boom")),
                ),
                FakeSettings(),
            )

        val outcome = service.search(LookupQuery("Bohemian Rhapsody", "Queen")) as LookupOutcome.Results

        assertEquals(1, outcome.candidates.size)
        assertEquals(listOf("deezer"), outcome.failedProviders)
    }

    @Test
    fun `offline everywhere is offline`() = runTest {
        val service =
            LookupService(
                listOf(Provider("musicbrainz", LookupResult.Offline), Provider("deezer", LookupResult.Offline)),
                FakeSettings(),
            )
        assertEquals(LookupOutcome.Offline, service.search(LookupQuery("x")))
    }

    @Test
    fun `proposals compare the file with the candidate`() {
        val current = TagMap.of("TITLE" to listOf("bohemian rhapsody"), "ARTIST" to listOf("Queen"))
        val proposals =
            proposalsFor(
                LookupCandidate(
                    providerId = "mb",
                    providerName = "mb",
                    id = "1",
                    title = "Bohemian Rhapsody",
                    artists = listOf("Queen"),
                    album = "A Night at the Opera",
                    trackNumber = 11,
                    trackTotal = 12,
                    identifiers = mapOf("MUSICBRAINZ_TRACKID" to "abc"),
                ),
                current,
                PositionStyle.Combined,
            ).associateBy { it.key }

        assertTrue(proposals.getValue("TITLE").differs)
        assertTrue(!proposals.getValue("ARTIST").differs)
        assertEquals(listOf("11/12"), proposals.getValue("TRACKNUMBER").proposed)
        assertEquals(listOf("abc"), proposals.getValue("MUSICBRAINZ_TRACKID").proposed)
        assertTrue("GENRE" !in proposals)
    }
}
