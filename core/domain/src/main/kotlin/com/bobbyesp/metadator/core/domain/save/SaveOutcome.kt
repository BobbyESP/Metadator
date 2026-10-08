/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.tags.api.BackupId

/** How a save ended. What the user can cause or fix is a result here, not an exception. */
sealed interface SaveOutcome {
    /**
     * Written. [notStored] are changed keys the format could not hold as given (a custom key in a
     * WAV, a full date in ID3v2.3): the rest of the save stands.
     */
    data class Saved(val backupId: BackupId, val notStored: Set<String> = emptySet()) : SaveOutcome

    data object NothingToSave : SaveOutcome

    /** The user has to grant write access first. Nothing was written. */
    data object NeedsAccess : SaveOutcome

    /** The file is gone or moved. Nothing was written. */
    data object FileGone : SaveOutcome

    /** TagLib cannot write this format. Nothing was written. */
    data object Unsupported : SaveOutcome

    /**
     * The write failed. [restored] says whether the original tags were put back; if not, the backup
     * is kept so the user can restore it from the editor's history.
     */
    data class Failed(val message: String, val restored: Boolean) : SaveOutcome
}
