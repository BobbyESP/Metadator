/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.tags.api

import com.bobbyesp.metadator.core.model.ContentRef

/** Reads tags from audio files. */
interface TagReader {
    suspend fun read(ref: ContentRef, includePictures: Boolean = true): TagReadResult

    suspend fun readAudioProperties(ref: ContentRef): AudioProperties?
}

sealed interface TagReadResult {
    data class Success(val snapshot: TagSnapshot) : TagReadResult

    data object NotFound : TagReadResult

    data object AccessDenied : TagReadResult

    /** The file opened but has no tag format TagLib can read. */
    data object Unsupported : TagReadResult

    data class Failed(val message: String) : TagReadResult
}

/** Writes tags to audio files. */
interface TagWriter {
    /**
     * Replaces the file's tags with [tags] (a key missing from it is removed) and, when [pictures]
     * is not null, its pictures with [pictures]. Callers pass the original map with the user's
     * changes applied, never a map built from scratch.
     */
    suspend fun write(
        ref: ContentRef,
        tags: TagMap,
        pictures: List<EmbeddedPicture>?,
    ): TagWriteResult
}

sealed interface TagWriteResult {
    data object Written : TagWriteResult

    /** The app may not write this file yet; the user can grant it. */
    data object NeedsAccess : TagWriteResult

    data object NotFound : TagWriteResult

    data class Failed(val message: String) : TagWriteResult
}
