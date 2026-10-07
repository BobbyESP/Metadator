/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.lyrics

import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.lyrics.api.SongLyrics
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import kotlinx.coroutines.withContext

/** The lyrics of a song as the player shows them. */
sealed interface TrackLyrics {
    data class Available(val lyrics: SongLyrics) : TrackLyrics

    /** The file was read and has no lyrics. */
    data object None : TrackLyrics

    /** The file could not be read, so whether it has lyrics is not known. */
    data object Unreadable : TrackLyrics
}

/**
 * The lyrics embedded in a file, for the player. Only what the file has: nothing is looked up
 * online from here, since nothing leaves the device unless the user asks, and that is the editor's
 * "Find lyrics".
 */
class LoadLyricsUseCase(
    private val reader: TagReader,
    private val dispatchers: AppDispatchers,
) {
    suspend operator fun invoke(ref: ContentRef): TrackLyrics =
        when (val read = reader.read(ref, includePictures = false)) {
            is TagReadResult.Success -> {
                val text = read.snapshot.tags.first(TagField.Lyrics.key)
                val lyrics = text?.let { withContext(dispatchers.default) { SongLyrics.parse(it) } }
                if (lyrics == null) TrackLyrics.None else TrackLyrics.Available(lyrics)
            }
            else -> TrackLyrics.Unreadable
        }
}
