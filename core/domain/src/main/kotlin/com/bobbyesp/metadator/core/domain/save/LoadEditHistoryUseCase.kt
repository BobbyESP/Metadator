/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.save

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagSnapshot

/**
 * A song's saves, newest first, each with what it changed. A backup holds the file as it was before
 * a save, so what that save changed is the difference to the version that followed it: the next
 * backup, or the file as it is now for the newest one.
 */
class LoadEditHistoryUseCase(private val backups: TagBackupStore) {
    suspend operator fun invoke(ref: ContentRef, current: TagSnapshot): List<EditHistoryEntry> {
        val history = backups.history(ref).sortedByDescending { it.createdAtMillis }
        return history.mapIndexed { index, backup ->
            val after = if (index == 0) current else history[index - 1].snapshot
            EditHistoryEntry(
                id = backup.id,
                savedAtMillis = backup.createdAtMillis,
                cover = backup.snapshot.frontCover,
                fields = fieldChanges(backup.snapshot, after),
                coverChange = coverChange(backup.snapshot.ownCover, after.ownCover),
            )
        }
    }

    /**
     * The picture a cover change replaces or removes, as writing tells it: no stand-in from the
     * other pictures, or removing the cover would read as replacing it with the back cover.
     */
    private val TagSnapshot.ownCover: EmbeddedPicture?
        get() =
            pictures.firstOrNull { it.type == PictureType.FrontCover }
                ?: pictures.singleOrNull()?.takeIf { it.type == PictureType.Other }

    private fun fieldChanges(before: TagSnapshot, after: TagSnapshot): List<FieldChange> =
        (before.tags.keys + after.tags.keys)
            .filter { before.tags[it] != after.tags[it] }
            .sortedWith(compareBy({ FieldOrder[it] ?: Int.MAX_VALUE }, { it }))
            .map { FieldChange(it, before.tags[it], after.tags[it]) }

    private fun coverChange(before: EmbeddedPicture?, after: EmbeddedPicture?): CoverChange? =
        when {
            before == after -> null
            before == null -> CoverChange.Added
            after == null -> CoverChange.Removed
            else -> CoverChange.Replaced
        }

    private companion object {
        /** Known fields in the order the editor shows them; the rest after, by name. */
        val FieldOrder: Map<String, Int> =
            TagField.entries.withIndex().associate { (index, field) -> field.key to index }
    }
}

/**
 * One save of a song. [cover] is the front cover the song had before it, the one restoring brings
 * back; [fields] and [coverChange] are what the save changed.
 */
data class EditHistoryEntry(
    val id: BackupId,
    val savedAtMillis: Long,
    val cover: EmbeddedPicture?,
    val fields: List<FieldChange>,
    val coverChange: CoverChange?,
)

/** A key as it was before a save and after it; an empty list is a key that was not there. */
data class FieldChange(val key: String, val before: List<String>, val after: List<String>)

enum class CoverChange {
    Added,
    Replaced,
    Removed,
}
