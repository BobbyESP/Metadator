/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.library.api

import com.bobbyesp.metadator.core.model.TrackId

/** What a file's own tags say about the fields the library lists a song by. */
data class FileTrackTags(
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val genre: String? = null,
)

/** The tags of a file as it was at one modification date and size; stale once either changes. */
data class CachedTrackTags(
    val id: TrackId,
    val dateModifiedEpochSeconds: Long,
    val sizeBytes: Long,
    val tags: FileTrackTags,
)

/** Remembers what was read from each file, so a file is opened once per version of it. */
interface TrackTagCache {
    suspend fun all(): List<CachedTrackTags>

    suspend fun put(entries: List<CachedTrackTags>)

    suspend fun remove(ids: Collection<TrackId>)
}
