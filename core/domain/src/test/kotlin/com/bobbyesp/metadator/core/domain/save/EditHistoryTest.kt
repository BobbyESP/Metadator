/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.core.domain.FakeBackupStore
import com.bobbyesp.metadator.core.domain.FakeIndexer
import com.bobbyesp.metadator.core.domain.FakeTagFiles
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.tags.api.ArtworkChange
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagChanges
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import com.bobbyesp.metadator.tags.api.TagWriteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {
    private val ref = ContentRef("content://media/external/audio/media/1")
    private val front =
        EmbeddedPicture(byteArrayOf(1, 2), "image/jpeg", type = PictureType.FrontCover)
    private val back = EmbeddedPicture(byteArrayOf(3), "image/jpeg", type = PictureType.BackCover)
    private val original =
        TagSnapshot(
            TagMap.of(
                "TITLE" to listOf("Halo"),
                "ARTIST" to listOf("Beyoncé"),
                "MOOD" to listOf("Warm"),
            ),
            listOf(front, back),
        )

    private val files = FakeTagFiles(mapOf(ref to original))
    private val backups = FakeBackupStore()
    private val indexer = FakeIndexer()
    private var now = 0L
    private val save = SaveTagChangesUseCase(files, files, backups, indexer, clock = { ++now })
    private val restore = RestoreBackupUseCase(backups, files, files, indexer, clock = { ++now })
    private val history = LoadEditHistoryUseCase(backups)

    private suspend fun saveFields(vararg fields: Pair<String, List<String>>) =
        save(ref, TagChanges(fields = fields.toMap())) as SaveOutcome.Saved

    private fun current() = files.files.getValue(ref)

    @Test
    fun `each entry says what its save changed, newest first`() = runTest {
        saveFields("TITLE" to listOf("Halo (Live)"))
        saveFields("ARTIST" to listOf("Beyoncé", "Jay-Z"), "MOOD" to emptyList())

        val entries = history(ref, current())

        assertEquals(2, entries.size)
        assertEquals(
            listOf(
                FieldChange("ARTIST", listOf("Beyoncé"), listOf("Beyoncé", "Jay-Z")),
                FieldChange("MOOD", listOf("Warm"), emptyList()),
            ),
            entries[0].fields,
        )
        assertEquals(
            listOf(FieldChange("TITLE", listOf("Halo"), listOf("Halo (Live)"))),
            entries[1].fields,
        )
        assertTrue(entries[0].savedAtMillis > entries[1].savedAtMillis)
    }

    @Test
    fun `known fields come in the editor's order, others after by name`() = runTest {
        saveFields(
            "ZZZ" to listOf("1"),
            "AAA" to listOf("1"),
            "ARTIST" to listOf("x"),
            "TITLE" to listOf("y"),
        )

        assertEquals(
            listOf("TITLE", "ARTIST", "AAA", "ZZZ"),
            history(ref, current()).single().fields.map { it.key },
        )
    }

    @Test
    fun `a cover change is told apart from the other pictures`() = runTest {
        val newFront = EmbeddedPicture(byteArrayOf(9), "image/png", type = PictureType.FrontCover)
        save(ref, TagChanges(artwork = ArtworkChange.Replace(newFront)))
        save(ref, TagChanges(artwork = ArtworkChange.Remove))

        val entries = history(ref, current())

        assertEquals(CoverChange.Removed, entries[0].coverChange)
        assertEquals(newFront, entries[0].cover)
        assertEquals(CoverChange.Replaced, entries[1].coverChange)
        assertEquals(front, entries[1].cover)
        assertTrue(entries.all { it.fields.isEmpty() })
    }

    @Test
    fun `restoring an older version brings it back and can itself be undone`() = runTest {
        val first = saveFields("TITLE" to listOf("Halo (Live)"))
        saveFields("ARTIST" to listOf("Jay-Z"))
        val beforeRestore = current()

        val outcome = restore(first.backupId) as RestoreOutcome.Restored

        assertEquals(original, current())
        val previous = checkNotNull(outcome.previous)
        assertEquals(beforeRestore, backups.backups.getValue(previous).snapshot)
        assertTrue(first.backupId !in backups.backups)
        assertEquals(listOf(ref, ref, ref), indexer.rescanned)

        assertTrue(restore(previous) is RestoreOutcome.Restored)
        assertEquals(beforeRestore, current())
    }

    @Test
    fun `restoring the version the file already has writes nothing`() = runTest {
        val saved = saveFields("TITLE" to listOf("Halo (Live)"))
        files.files[ref] = original
        val writes = files.writes.size

        val outcome = restore(saved.backupId) as RestoreOutcome.Restored

        assertNull(outcome.previous)
        assertEquals(writes, files.writes.size)
        assertTrue(backups.backups.isEmpty())
    }

    @Test
    fun `a failed restore leaves the file and the history as they were`() = runTest {
        val saved = saveFields("TITLE" to listOf("Halo (Live)"))
        val before = current()
        files.failNextWrites = 1

        assertTrue(restore(saved.backupId) is RestoreOutcome.Failed)

        assertEquals(before, current())
        assertEquals(setOf(saved.backupId), backups.backups.keys)
    }

    @Test
    fun `a restore without write access keeps no extra backup`() = runTest {
        val saved = saveFields("TITLE" to listOf("Halo (Live)"))
        files.writeResult = TagWriteResult.NeedsAccess

        assertEquals(RestoreOutcome.NeedsAccess, restore(saved.backupId))
        assertEquals(setOf(saved.backupId), backups.backups.keys)
    }
}
