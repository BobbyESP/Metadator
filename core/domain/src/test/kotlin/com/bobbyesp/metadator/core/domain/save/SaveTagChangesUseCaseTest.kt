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
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveTagChangesUseCaseTest {
    private val ref = ContentRef("content://media/external/audio/media/1")
    private val front = EmbeddedPicture(byteArrayOf(1, 2), "image/jpeg", type = PictureType.FrontCover)
    private val back = EmbeddedPicture(byteArrayOf(3), "image/jpeg", type = PictureType.BackCover)
    private val original =
        TagSnapshot(
            TagMap.of(
                "TITLE" to listOf("Halo"),
                "ARTIST" to listOf("Beyoncé"),
                "REPLAYGAIN_TRACK_GAIN" to listOf("-7.2 dB"),
                "MUSICBRAINZ_TRACKID" to listOf("abc"),
            ),
            listOf(front, back),
        )

    private val files = FakeTagFiles(mapOf(ref to original))
    private val backups = FakeBackupStore()
    private val indexer = FakeIndexer()
    private val save = SaveTagChangesUseCase(files, files, backups, indexer, clock = { 42 })

    @Test
    fun `saving one field keeps every other tag and every picture`() = runTest {
        val outcome = save(ref, TagChanges(fields = mapOf("TITLE" to listOf("Halo (Live)"))))

        assertTrue(outcome is SaveOutcome.Saved)
        val stored = files.files.getValue(ref)
        assertEquals(listOf("Halo (Live)"), stored.tags["TITLE"])
        assertEquals(listOf("-7.2 dB"), stored.tags["REPLAYGAIN_TRACK_GAIN"])
        assertEquals(listOf("abc"), stored.tags["MUSICBRAINZ_TRACKID"])
        assertEquals(listOf(front, back), stored.pictures)
    }

    @Test
    fun `the original is backed up and the file rescanned`() = runTest {
        val outcome = save(ref, TagChanges(fields = mapOf("TITLE" to listOf("x")))) as SaveOutcome.Saved

        assertEquals(original, backups.backups.getValue(outcome.backupId).snapshot)
        assertEquals(listOf(ref), indexer.rescanned)
    }

    @Test
    fun `changes apply to the file as it is now, not as the editor read it`() = runTest {
        // Someone else changed the album after the editor opened.
        files.files[ref] = original.copy(tags = original.tags.with("ALBUM", listOf("I Am... Sasha Fierce")))

        save(ref, TagChanges(fields = mapOf("TITLE" to listOf("x"))))

        assertEquals(listOf("I Am... Sasha Fierce"), files.files.getValue(ref).tags["ALBUM"])
    }

    @Test
    fun `replacing the cover keeps the back cover`() = runTest {
        val cover = EmbeddedPicture(byteArrayOf(9), "image/png")
        save(ref, TagChanges(artwork = ArtworkChange.Replace(cover)))

        val pictures = files.files.getValue(ref).pictures
        assertEquals(2, pictures.size)
        assertTrue(pictures.first().data.contentEquals(byteArrayOf(9)))
        assertEquals(back, pictures[1])
    }

    @Test
    fun `nothing changed means nothing written`() = runTest {
        assertEquals(SaveOutcome.NothingToSave, save(ref, TagChanges()))
        assertEquals(
            SaveOutcome.NothingToSave,
            save(ref, TagChanges(fields = mapOf("TITLE" to listOf("Halo")))),
        )
        assertTrue(files.writes.isEmpty())
        assertTrue(backups.backups.isEmpty())
    }

    @Test
    fun `needing access writes nothing and keeps no backup`() = runTest {
        files.writeResult = TagWriteResult.NeedsAccess

        assertEquals(SaveOutcome.NeedsAccess, save(ref, TagChanges(fields = mapOf("TITLE" to listOf("x")))))
        assertTrue(backups.backups.isEmpty())
    }

    @Test
    fun `a failed write puts the original back`() = runTest {
        files.failNextWrites = 1

        val outcome = save(ref, TagChanges(fields = mapOf("TITLE" to listOf("x"))))

        assertEquals(SaveOutcome.Failed("disk on fire", restored = true), outcome)
        assertEquals(original, files.files.getValue(ref))
        assertTrue(backups.backups.isEmpty())
    }

    @Test
    fun `a failed write that cannot be undone keeps the backup`() = runTest {
        files.failNextWrites = 2

        val outcome = save(ref, TagChanges(fields = mapOf("TITLE" to listOf("x"))))

        assertEquals(SaveOutcome.Failed("disk on fire", restored = false), outcome)
        assertEquals(1, backups.backups.size)
    }

    @Test
    fun `keys the format drops are reported, the rest stands`() = runTest {
        val limited = FakeTagFiles(mapOf(ref to original), unsupportedKeys = setOf("MOOD"))
        val saveLimited = SaveTagChangesUseCase(limited, limited, backups, indexer)

        val outcome =
            saveLimited(
                ref,
                TagChanges(fields = mapOf("MOOD" to listOf("calm"), "TITLE" to listOf("y"))),
            ) as SaveOutcome.Saved

        assertEquals(setOf("MOOD"), outcome.notStored)
        assertEquals(listOf("y"), limited.files.getValue(ref).tags["TITLE"])
    }

    @Test
    fun `a missing file is reported, not thrown`() = runTest {
        val outcome = save(ContentRef("content://gone"), TagChanges(fields = mapOf("TITLE" to listOf("x"))))
        assertEquals(SaveOutcome.FileGone, outcome)
    }

    @Test
    fun `undo restores tags and pictures`() = runTest {
        val outcome =
            save(
                ref,
                TagChanges(
                    fields = mapOf("TITLE" to listOf("x"), "REPLAYGAIN_TRACK_GAIN" to emptyList()),
                    artwork = ArtworkChange.Remove,
                ),
            ) as SaveOutcome.Saved

        val restore = RestoreBackupUseCase(backups, files, indexer)
        assertEquals(RestoreOutcome.Restored, restore(outcome.backupId))
        assertEquals(original, files.files.getValue(ref))
        assertTrue(backups.backups.isEmpty())
    }
}
