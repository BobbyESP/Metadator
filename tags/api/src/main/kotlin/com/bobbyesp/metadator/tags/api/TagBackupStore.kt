/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.tags.api

import com.bobbyesp.metadator.core.model.ContentRef

/**
 * The tags and pictures of a file as they were before Metadator wrote it, kept so a save can be
 * undone. Bounded: the oldest backups go first.
 */
interface TagBackupStore {
    suspend fun save(ref: ContentRef, snapshot: TagSnapshot, createdAtMillis: Long): BackupId

    suspend fun load(id: BackupId): TagBackup?

    suspend fun history(ref: ContentRef): List<TagBackup>

    suspend fun delete(id: BackupId)

    suspend fun clear()
}

@JvmInline value class BackupId(val value: Long)

data class TagBackup(
    val id: BackupId,
    val ref: ContentRef,
    val snapshot: TagSnapshot,
    val createdAtMillis: Long,
)
