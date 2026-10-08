/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.library.api.MediaIndexer
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagWriteResult
import com.bobbyesp.metadator.tags.api.TagWriter

/** Puts a file's tags and pictures back as they were before a save: what Undo does. */
class RestoreBackupUseCase(
    private val backups: TagBackupStore,
    private val writer: TagWriter,
    private val indexer: MediaIndexer,
) {
    suspend operator fun invoke(id: BackupId): RestoreOutcome {
        val backup = backups.load(id) ?: return RestoreOutcome.BackupGone
        return when (
            val result = writer.write(backup.ref, backup.snapshot.tags, backup.snapshot.pictures)
        ) {
            TagWriteResult.Written -> {
                backups.delete(id)
                indexer.rescan(listOf(backup.ref))
                RestoreOutcome.Restored
            }
            TagWriteResult.NeedsAccess -> RestoreOutcome.NeedsAccess
            TagWriteResult.NotFound -> RestoreOutcome.FileGone
            is TagWriteResult.Failed -> RestoreOutcome.Failed(result.message)
        }
    }
}

sealed interface RestoreOutcome {
    data object Restored : RestoreOutcome

    data object BackupGone : RestoreOutcome

    data object NeedsAccess : RestoreOutcome

    data object FileGone : RestoreOutcome

    data class Failed(val message: String) : RestoreOutcome
}
