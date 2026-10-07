package com.bobbyesp.metadator.core.domain.editor

import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.library.api.AudioFileInfo
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.library.api.FileDescription
import com.bobbyesp.metadator.tags.api.AudioProperties
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Everything the editor shows about one file. */
data class LoadedTrack(
    val ref: ContentRef,
    /** The library's entry, when the file is in the library. */
    val track: Track?,
    val file: FileDescription?,
    val snapshot: TagSnapshot,
    val audio: AudioProperties?,
) {
    val fileExtension: String?
        get() =
            (track?.displayName ?: file?.displayName)
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase()
                ?.ifEmpty { null }
}

sealed interface LoadTrackResult {
    data class Loaded(val track: LoadedTrack) : LoadTrackResult

    data object NotFound : LoadTrackResult

    data object AccessDenied : LoadTrackResult

    data object Unsupported : LoadTrackResult

    data class Failed(val message: String) : LoadTrackResult
}

class LoadTrackUseCase(
    private val library: AudioLibrary,
    private val fileInfo: AudioFileInfo,
    private val reader: TagReader,
) {
    suspend operator fun invoke(ref: ContentRef): LoadTrackResult = coroutineScope {
        val track = async { library.findByRef(ref) }
        val file = async { fileInfo.describe(ref) }
        val audio = async { reader.readAudioProperties(ref) }
        when (val read = reader.read(ref, includePictures = true)) {
            is TagReadResult.Success ->
                LoadTrackResult.Loaded(
                    LoadedTrack(ref, track.await(), file.await(), read.snapshot, audio.await())
                )
            TagReadResult.NotFound -> LoadTrackResult.NotFound
            TagReadResult.AccessDenied -> LoadTrackResult.AccessDenied
            TagReadResult.Unsupported -> LoadTrackResult.Unsupported
            is TagReadResult.Failed -> LoadTrackResult.Failed(read.message)
        }
    }
}
