package com.bobbyesp.metadator.library.api

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import kotlinx.coroutines.flow.Flow

/** The songs on the device, kept up to date as files come and go. */
interface AudioLibrary {
    /** Every song, re-emitted whenever the library changes. Needs the audio permission. */
    fun observeTracks(): Flow<List<Track>>

    suspend fun track(id: TrackId): Track?

    /** The library's entry for a file opened from elsewhere, when it is in the library. */
    suspend fun findByRef(ref: ContentRef): Track?

    /** Asks the system to look at the music folders again. */
    suspend fun refresh()
}

/** What can be known about any file the app is handed, library or not. */
interface AudioFileInfo {
    suspend fun describe(ref: ContentRef): FileDescription?
}

data class FileDescription(
    val displayName: String,
    val sizeBytes: Long?,
    val mimeType: String?,
    val lastModifiedMillis: Long?,
    /** Where the file is, for showing only. */
    val location: String?,
)
