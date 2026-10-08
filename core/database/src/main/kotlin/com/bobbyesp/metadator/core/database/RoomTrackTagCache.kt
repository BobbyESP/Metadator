/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.database

import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.library.api.CachedTrackTags
import com.bobbyesp.metadator.library.api.FileTrackTags
import com.bobbyesp.metadator.library.api.TrackTagCache

class RoomTrackTagCache(private val dao: TrackTagsDao) : TrackTagCache {

    override suspend fun all(): List<CachedTrackTags> =
        dao.all().map { row ->
            CachedTrackTags(
                id = TrackId(row.trackId),
                dateModifiedEpochSeconds = row.dateModified,
                sizeBytes = row.sizeBytes,
                tags =
                    FileTrackTags(
                        artist = row.artist,
                        album = row.album,
                        albumArtist = row.albumArtist,
                        year = row.year,
                        trackNumber = row.trackNumber,
                        discNumber = row.discNumber,
                        genre = row.genre,
                    ),
            )
        }

    override suspend fun put(entries: List<CachedTrackTags>) =
        dao.upsert(
            entries.map { entry ->
                TrackTagsEntity(
                    trackId = entry.id.value,
                    dateModified = entry.dateModifiedEpochSeconds,
                    sizeBytes = entry.sizeBytes,
                    artist = entry.tags.artist,
                    album = entry.tags.album,
                    albumArtist = entry.tags.albumArtist,
                    year = entry.tags.year,
                    trackNumber = entry.tags.trackNumber,
                    discNumber = entry.tags.discNumber,
                    genre = entry.tags.genre,
                )
            }
        )

    override suspend fun remove(ids: Collection<TrackId>) {
        // SQLite caps the variables of one statement at 999 on older Android versions.
        ids.map { it.value }.chunked(MAX_VARIABLES).forEach { dao.delete(it) }
    }

    private companion object {
        const val MAX_VARIABLES = 900
    }
}
