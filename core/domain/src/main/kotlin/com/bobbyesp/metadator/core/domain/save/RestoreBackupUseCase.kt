/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.library.api.MediaIndexer
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagWriteResult
import com.bobbyesp.metadator.tags.api.TagWriter

/**
 * Puts a file's tags and pictures back as they were before a save: what Undo and the editor's
 * history do. Restoring is a write like any other, so the file as it is now is backed up first and
 * the restore can itself be undone.
 */
class RestoreBackupUseCase(
    private val backups: TagBackupStore,
    private val reader: TagReader,
    private val writer: TagWriter,
    private val indexer: MediaIndexer,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(id: BackupId): RestoreOutcome {
        val backup = backups.load(id) ?: return RestoreOutcome.BackupGone
        val wanted = backup.snapshot
        val current =
            when (val read = reader.read(backup.ref, includePictures = true)) {
                is TagReadResult.Success -> read.snapshot
                TagReadResult.NotFound -> return RestoreOutcome.FileGone
                TagReadResult.AccessDenied -> return RestoreOutcome.NeedsAccess
                TagReadResult.Unsupported -> return RestoreOutcome.Failed("Unsupported format")
                is TagReadResult.Failed -> return RestoreOutcome.Failed(read.message)
            }
        if (current == wanted) {
            backups.delete(id)
            return RestoreOutcome.Restored(previous = null)
        }

        val previous = backups.save(backup.ref, current, clock())
        return when (val result = writer.write(backup.ref, wanted.tags, wanted.pictures)) {
            TagWriteResult.Written -> {
                backups.delete(id)
                indexer.rescan(listOf(backup.ref))
                RestoreOutcome.Restored(previous)
            }
            TagWriteResult.NeedsAccess -> {
                backups.delete(previous)
                RestoreOutcome.NeedsAccess
            }
            TagWriteResult.NotFound -> {
                backups.delete(previous)
                RestoreOutcome.FileGone
            }
            is TagWriteResult.Failed -> {
                // Whatever the failed write left, the file goes back to how it was; if that fails
                // too, the backup of it stays.
                if (
                    writer.write(backup.ref, current.tags, current.pictures) ==
                        TagWriteResult.Written
                )
                    backups.delete(previous)
                RestoreOutcome.Failed(result.message)
            }
        }
    }
}

sealed interface RestoreOutcome {
    /** [previous] is the backup of the file as it was just before, to undo the restore with. */
    data class Restored(val previous: BackupId?) : RestoreOutcome

    data object BackupGone : RestoreOutcome

    data object NeedsAccess : RestoreOutcome

    data object FileGone : RestoreOutcome

    data class Failed(val message: String) : RestoreOutcome
}
