/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.database

import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagBackup
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Picture bytes of each backup, in `files/backups/<id>/`. */
class TagBackupFiles(private val root: File) {
    fun directory(id: Long): File = File(root, id.toString())

    fun delete(id: Long) {
        directory(id).deleteRecursively()
    }

    fun deleteAll() {
        root.deleteRecursively()
    }
}

class RoomTagBackupStore(
    private val dao: TagBackupDao,
    private val files: TagBackupFiles,
    private val dispatchers: AppDispatchers,
    /** The newest backups kept; older ones are dropped as new ones come in. */
    private val maxBackups: Int = 100,
) : TagBackupStore {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(
        ref: ContentRef,
        snapshot: TagSnapshot,
        createdAtMillis: Long,
    ): BackupId =
        withContext(dispatchers.io) {
            val pictures =
                snapshot.pictures.mapIndexed { index, picture ->
                    StoredPicture(
                        file = "$index",
                        mimeType = picture.mimeType,
                        description = picture.description,
                        type = picture.type.name,
                    )
                }
            val id =
                dao.insert(
                    TagBackupEntity(
                        uri = ref.uri,
                        createdAt = createdAtMillis,
                        tagsJson = json.encodeToString(snapshot.tags.toMap()),
                        picturesJson = json.encodeToString(pictures),
                    )
                )
            try {
                val directory = files.directory(id).apply { mkdirs() }
                snapshot.pictures.forEachIndexed { index, picture ->
                    File(directory, pictures[index].file).writeBytes(picture.data)
                }
            } catch (error: Exception) {
                // A backup without its pictures would restore a file without them.
                dao.delete(id)
                files.delete(id)
                throw error
            }
            prune()
            BackupId(id)
        }

    override suspend fun load(id: BackupId): TagBackup? =
        withContext(dispatchers.io) { dao.get(id.value)?.toBackup() }

    override suspend fun history(ref: ContentRef): List<TagBackup> =
        withContext(dispatchers.io) { dao.forUri(ref.uri).mapNotNull { it.toBackup() } }

    override suspend fun delete(id: BackupId) =
        withContext(dispatchers.io) {
            dao.delete(id.value)
            files.delete(id.value)
        }

    override suspend fun clear() =
        withContext(dispatchers.io) {
            dao.clear()
            files.deleteAll()
        }

    private suspend fun prune() {
        dao.idsBeyond(maxBackups).forEach { id ->
            dao.delete(id)
            files.delete(id)
        }
    }

    /** Null when the pictures of the backup are gone: restoring it would lose them. */
    private fun TagBackupEntity.toBackup(): TagBackup? {
        val tags = json.decodeFromString<Map<String, List<String>>>(tagsJson)
        val stored = json.decodeFromString<List<StoredPicture>>(picturesJson)
        val directory = files.directory(id)
        val pictures = stored.map { picture ->
            val file = File(directory, picture.file)
            if (!file.exists()) return null
            EmbeddedPicture(
                data = file.readBytes(),
                mimeType = picture.mimeType,
                description = picture.description,
                type =
                    runCatching { PictureType.valueOf(picture.type) }
                        .getOrDefault(PictureType.Other),
            )
        }
        return TagBackup(
            id = BackupId(id),
            ref = ContentRef(uri),
            snapshot = TagSnapshot(TagMap.of(tags), pictures),
            createdAtMillis = createdAt,
        )
    }
}

@Serializable
private data class StoredPicture(
    val file: String,
    val mimeType: String,
    val description: String,
    val type: String,
)
