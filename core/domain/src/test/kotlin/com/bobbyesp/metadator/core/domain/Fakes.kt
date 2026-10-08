/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain

import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.core.model.UserSettings
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.library.api.CachedTrackTags
import com.bobbyesp.metadator.library.api.MediaIndexer
import com.bobbyesp.metadator.library.api.TrackTagCache
import com.bobbyesp.metadator.tags.api.AudioProperties
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.TagBackup
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagSnapshot
import com.bobbyesp.metadator.tags.api.TagWriteResult
import com.bobbyesp.metadator.tags.api.TagWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Files in memory, read and written like TagLib does: a write replaces the whole property map, and
 * a key the format does not support is silently dropped.
 */
class FakeTagFiles(
    initial: Map<ContentRef, TagSnapshot> = emptyMap(),
    private val unsupportedKeys: Set<String> = emptySet(),
) : TagReader, TagWriter {
    val files = initial.toMutableMap()
    var writeResult: TagWriteResult? = null
    var failNextWrites = 0
    val writes = mutableListOf<ContentRef>()
    val reads = mutableListOf<ContentRef>()

    override suspend fun read(ref: ContentRef, includePictures: Boolean): TagReadResult {
        reads += ref
        val snapshot = files[ref] ?: return TagReadResult.NotFound
        return TagReadResult.Success(
            if (includePictures) snapshot else snapshot.copy(pictures = emptyList())
        )
    }

    override suspend fun readAudioProperties(ref: ContentRef) = AudioProperties(1000, 320, 44100, 2)

    override suspend fun write(
        ref: ContentRef,
        tags: TagMap,
        pictures: List<EmbeddedPicture>?,
    ): TagWriteResult {
        writeResult?.let {
            return it
        }
        if (failNextWrites > 0) {
            failNextWrites--
            return TagWriteResult.Failed("disk on fire")
        }
        val current = files[ref] ?: return TagWriteResult.NotFound
        writes += ref
        val stored = TagMap.of(tags.toMap().filterKeys { it !in unsupportedKeys })
        files[ref] = TagSnapshot(stored, pictures ?: current.pictures)
        return TagWriteResult.Written
    }
}

class FakeBackupStore : TagBackupStore {
    val backups = mutableMapOf<BackupId, TagBackup>()
    private var nextId = 1L

    override suspend fun save(
        ref: ContentRef,
        snapshot: TagSnapshot,
        createdAtMillis: Long,
    ): BackupId {
        val id = BackupId(nextId++)
        backups[id] = TagBackup(id, ref, snapshot, createdAtMillis)
        return id
    }

    override suspend fun load(id: BackupId) = backups[id]

    override suspend fun history(ref: ContentRef) = backups.values.filter { it.ref == ref }

    override suspend fun delete(id: BackupId) {
        backups.remove(id)
    }

    override suspend fun clear() = backups.clear()
}

class FakeIndexer : MediaIndexer {
    val rescanned = mutableListOf<ContentRef>()

    override suspend fun rescan(refs: List<ContentRef>) {
        rescanned += refs
    }
}

class FakeLibrary(initial: List<Track> = emptyList()) : AudioLibrary {
    val tracks = MutableStateFlow(initial)

    override fun observeTracks(): Flow<List<Track>> = tracks

    override suspend fun track(id: TrackId) = tracks.value.firstOrNull { it.id == id }

    override suspend fun findByRef(ref: ContentRef) = tracks.value.firstOrNull { it.ref == ref }

    override suspend fun refresh() = Unit
}

class FakeTrackTagCache(initial: List<CachedTrackTags> = emptyList()) : TrackTagCache {
    val entries = initial.associateByTo(mutableMapOf()) { it.id }

    override suspend fun all() = entries.values.toList()

    override suspend fun put(entries: List<CachedTrackTags>) {
        entries.forEach { this.entries[it.id] = it }
    }

    override suspend fun remove(ids: Collection<TrackId>) {
        ids.forEach(entries::remove)
    }
}

class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        settings.update(transform)
    }
}
