package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.library.api.MediaIndexer
import com.bobbyesp.metadator.tags.api.ArtworkChange
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagChanges
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagSnapshot
import com.bobbyesp.metadator.tags.api.TagWriteResult
import com.bobbyesp.metadator.tags.api.TagWriter
import com.bobbyesp.metadator.tags.api.applying

/**
 * Saves changes to a file without losing anything else in it:
 * 1. reads the file as it is now, so changes made elsewhere since the editor opened survive;
 * 2. applies only the changed keys and, if asked, the cover;
 * 3. backs up what was there, for undo;
 * 4. writes, and on failure tries to put the original back;
 * 5. reads again to check what the format really stored;
 * 6. asks the system to index the file again.
 */
class SaveTagChangesUseCase(
    private val reader: TagReader,
    private val writer: TagWriter,
    private val backups: TagBackupStore,
    private val indexer: MediaIndexer,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(ref: ContentRef, changes: TagChanges): SaveOutcome =
        invoke(ref) { changes }

    /** For changes that depend on the file's current tags, as batch numbering does. */
    suspend operator fun invoke(ref: ContentRef, changesFor: (TagSnapshot) -> TagChanges): SaveOutcome {
        val current =
            when (val read = reader.read(ref, includePictures = true)) {
                is TagReadResult.Success -> read.snapshot
                TagReadResult.NotFound -> return SaveOutcome.FileGone
                TagReadResult.AccessDenied -> return SaveOutcome.NeedsAccess
                TagReadResult.Unsupported -> return SaveOutcome.Unsupported
                is TagReadResult.Failed -> return SaveOutcome.Failed(read.message, restored = true)
            }

        val changes = changesFor(current)
        if (changes.isEmpty) return SaveOutcome.NothingToSave
        val target = current.applying(changes)
        if (target.tags == current.tags && (target.pictures == null || target.pictures == current.pictures)) {
            return SaveOutcome.NothingToSave
        }

        val backupId = backups.save(ref, current, clock())

        when (val write = writer.write(ref, target.tags, target.pictures)) {
            TagWriteResult.Written -> Unit
            TagWriteResult.NeedsAccess -> {
                backups.delete(backupId)
                return SaveOutcome.NeedsAccess
            }
            TagWriteResult.NotFound -> {
                backups.delete(backupId)
                return SaveOutcome.FileGone
            }
            is TagWriteResult.Failed -> {
                val restored =
                    writer.write(ref, current.tags, current.pictures) == TagWriteResult.Written
                if (restored) backups.delete(backupId)
                return SaveOutcome.Failed(write.message, restored)
            }
        }

        val verify =
            reader.read(ref, includePictures = changes.artwork != ArtworkChange.Unchanged)
        val notStored =
            if (verify is TagReadResult.Success) {
                changes.changedKeys.filterTo(mutableSetOf()) { key ->
                    verify.snapshot.tags[key] != target.tags[key]
                }
            } else emptySet()

        indexer.rescan(listOf(ref))
        return SaveOutcome.Saved(backupId, notStored)
    }
}
