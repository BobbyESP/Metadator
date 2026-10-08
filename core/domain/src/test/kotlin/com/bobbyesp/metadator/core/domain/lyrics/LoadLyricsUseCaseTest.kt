/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.lyrics

import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.domain.FakeTagFiles
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.lyrics.api.SongLyrics
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadLyricsUseCaseTest {
    private val plain = ContentRef("content://media/external/audio/media/1")
    private val synced = ContentRef("content://media/external/audio/media/2")
    private val silent = ContentRef("content://media/external/audio/media/3")
    private val missing = ContentRef("content://media/external/audio/media/4")

    private val files =
        FakeTagFiles(
            mapOf(
                plain to snapshot("LYRICS" to listOf("Just words")),
                synced to snapshot("LYRICS" to listOf("[00:01.00]<00:01.00>One <00:01.50>two")),
                silent to snapshot("TITLE" to listOf("Interlude")),
            )
        )

    @Test
    fun `plain lyrics are available as text`() = runTest {
        assertEquals(TrackLyrics.Available(SongLyrics.Plain("Just words")), load(plain))
    }

    @Test
    fun `timed lyrics are available with their words`() = runTest {
        val lyrics = (load(synced) as TrackLyrics.Available).lyrics as SongLyrics.Synced

        assertTrue(lyrics.isWordByWord)
        assertEquals("One two", lyrics.lines.single().text)
    }

    @Test
    fun `a file without lyrics has none`() = runTest {
        assertEquals(TrackLyrics.None, load(silent))
    }

    @Test
    fun `a file that cannot be read is told apart from one without lyrics`() = runTest {
        assertEquals(TrackLyrics.Unreadable, load(missing))
    }

    private suspend fun kotlinx.coroutines.test.TestScope.load(ref: ContentRef): TrackLyrics {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return LoadLyricsUseCase(files, AppDispatchers(dispatcher, dispatcher))(ref)
    }

    private fun snapshot(vararg tags: Pair<String, List<String>>) =
        TagSnapshot(TagMap.of(*tags), emptyList())
}
