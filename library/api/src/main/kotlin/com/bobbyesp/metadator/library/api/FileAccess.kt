package com.bobbyesp.metadator.library.api

import com.bobbyesp.metadator.core.model.ContentRef

/** Opens audio files for native code, which reads and writes through a file descriptor. */
interface AudioFileOpener {
    fun open(ref: ContentRef, mode: AccessMode): OpenResult
}

enum class AccessMode {
    Read,
    ReadWrite,
}

sealed interface OpenResult {
    data class Opened(val file: OpenedAudioFile) : OpenResult

    data object NotFound : OpenResult

    /** Writing needs the user's permission first ([WriteAccess]). */
    data object AccessDenied : OpenResult

    data class Failed(val message: String) : OpenResult
}

/** An open file. Close it when done; the descriptors it hands out are the caller's to close. */
interface OpenedAudioFile : AutoCloseable {
    /**
     * A new descriptor for the same file, detached from this object: native code takes ownership
     * and closes it.
     */
    fun detachDuplicateFd(): Int
}

/**
 * Getting the user's permission to change files the app did not create. The prompt depends on the
 * Android version: a runtime permission up to 9, a per-file prompt on 10, and one prompt for any
 * number of files from 11.
 */
interface WriteAccess {
    /** Whether [refs] can be written now, without asking. */
    suspend fun canWrite(refs: List<ContentRef>): Boolean

    /** Asks for whatever is missing. Suspends while the system prompt is on screen. */
    suspend fun request(refs: List<ContentRef>): AccessResult
}

enum class AccessResult {
    Granted,
    Denied,
    /** No screen could show the prompt (the app is in the background). */
    Unavailable,
}

/** Makes the system index files again after they were written, so every app sees the new tags. */
interface MediaIndexer {
    suspend fun rescan(refs: List<ContentRef>)
}
