/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.batch

import com.bobbyesp.metadator.core.domain.FakeBackupStore
import com.bobbyesp.metadator.core.domain.FakeIndexer
import com.bobbyesp.metadator.core.domain.FakeTagFiles
import com.bobbyesp.metadator.core.domain.save.SaveTagChangesUseCase
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BatchTest {

    @Test
    fun `patterns read tags out of file names`() {
        val pattern = FileNamePattern("{track} - {artist} - {title}")
        assertEquals(
            mapOf(
                "TRACKNUMBER" to listOf("03"),
                "ARTIST" to listOf("AC/DC"),
                "TITLE" to listOf("Back In Black - Live"),
            ),
            pattern.parse("03 - AC/DC - Back In Black - Live.flac"),
        )
        assertNull(pattern.parse("no separators here.mp3"))
    }

    @Test
    fun `patterns format file names from tags`() {
        val tags =
            TagMap.of(
                "ARTIST" to listOf("AC/DC"),
                "TITLE" to listOf("Hells Bells"),
                "TRACKNUMBER" to listOf("1/10"),
            )
        assertEquals(
            "1 - AC_DC - Hells Bells",
            FileNamePattern("{track} - {artist} - {title}").format(tags),
        )
        assertNull(FileNamePattern("{album} - {title}").format(tags))
    }

    @Test
    fun `invalid patterns are recognised`() {
        assertFalse(FileNamePattern("no placeholders").isValid)
        assertFalse(FileNamePattern("{title} {title}").isValid)
        assertFalse(FileNamePattern("{nope}").isValid)
    }

    @Test
    fun `a batch sets common fields, numbers tracks and keeps the rest`() = runTest {
        val refs = (1..3).map { ContentRef("content://$it") }
        val files =
            FakeTagFiles(
                refs.associateWith {
                    TagSnapshot(
                        TagMap.of("TITLE" to listOf(it.uri), "BPM" to listOf("120")),
                        emptyList(),
                    )
                }
            )
        val runner =
            BatchRunner(this, SaveTagChangesUseCase(files, files, FakeBackupStore(), FakeIndexer()))

        runner.start(
            refs.mapIndexed { index, ref -> BatchItem(ref, "song$index.mp3") },
            BatchEdit(fields = mapOf("ALBUM" to listOf("Live")), numbering = TrackNumbering()),
        )
        advanceUntilIdle()

        val progress = runner.progress.value!!
        assertFalse(progress.isRunning)
        assertEquals(3, progress.saved.size)
        refs.forEachIndexed { index, ref ->
            val tags = files.files.getValue(ref).tags
            assertEquals(listOf("Live"), tags["ALBUM"])
            assertEquals(listOf("${index + 1}/3"), tags["TRACKNUMBER"])
            assertEquals(listOf("120"), tags["BPM"])
            assertEquals(listOf(ref.uri), tags["TITLE"])
        }
    }
}
