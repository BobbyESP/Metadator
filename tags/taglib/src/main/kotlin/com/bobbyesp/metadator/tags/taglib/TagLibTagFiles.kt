package com.bobbyesp.metadator.tags.taglib

import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.library.api.AccessMode
import com.bobbyesp.metadator.library.api.AudioFileOpener
import com.bobbyesp.metadator.library.api.OpenResult
import com.bobbyesp.metadator.library.api.OpenedAudioFile
import com.bobbyesp.metadator.tags.api.AudioProperties
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagSnapshot
import com.bobbyesp.metadator.tags.api.TagWriteResult
import com.bobbyesp.metadator.tags.api.TagWriter
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withContext

/**
 * Reads and writes tags with TagLib, through file descriptors the [opener] hands out.
 *
 * Every TagLib call takes a fresh duplicate descriptor, because TagLib closes the one it is
 * given. Calls are blocking native code, so they run on the IO dispatcher.
 */
class TagLibTagFiles(
    private val opener: AudioFileOpener,
    private val dispatchers: AppDispatchers,
) : TagReader, TagWriter {

    override suspend fun read(ref: ContentRef, includePictures: Boolean): TagReadResult =
        withContext(dispatchers.io) {
            when (val open = opener.open(ref, AccessMode.Read)) {
                OpenResult.NotFound -> TagReadResult.NotFound
                OpenResult.AccessDenied -> TagReadResult.AccessDenied
                is OpenResult.Failed -> TagReadResult.Failed(open.message)
                is OpenResult.Opened ->
                    open.file.use { file ->
                        guarded(onError = { TagReadResult.Failed(it) }) {
                            val metadata =
                                TagLib.getMetadata(file.detachDuplicateFd(), includePictures)
                                    ?: return@guarded TagReadResult.Unsupported
                            TagReadResult.Success(
                                TagSnapshot(
                                    tags = metadata.propertyMap.toTagMap(),
                                    pictures = metadata.pictures.map { it.toEmbeddedPicture() },
                                )
                            )
                        }
                    }
            }
        }

    override suspend fun readAudioProperties(ref: ContentRef): AudioProperties? =
        withContext(dispatchers.io) {
            val open = opener.open(ref, AccessMode.Read) as? OpenResult.Opened ?: return@withContext null
            open.file.use { file ->
                guarded(onError = { null }) {
                    TagLib.getAudioProperties(file.detachDuplicateFd(), AudioPropertiesReadStyle.Average)
                        ?.let {
                            AudioProperties(
                                durationMs = it.length.toLong(),
                                bitrateKbps = it.bitrate,
                                sampleRateHz = it.sampleRate,
                                channels = it.channels,
                            )
                        }
                }
            }
        }

    override suspend fun write(
        ref: ContentRef,
        tags: TagMap,
        pictures: List<EmbeddedPicture>?,
    ): TagWriteResult =
        withContext(dispatchers.io) {
            when (val open = opener.open(ref, AccessMode.ReadWrite)) {
                OpenResult.NotFound -> TagWriteResult.NotFound
                OpenResult.AccessDenied -> TagWriteResult.NeedsAccess
                is OpenResult.Failed -> TagWriteResult.Failed(open.message)
                is OpenResult.Opened -> open.file.use { file -> write(file, tags, pictures) }
            }
        }

    private fun write(
        file: OpenedAudioFile,
        tags: TagMap,
        pictures: List<EmbeddedPicture>?,
    ): TagWriteResult =
        guarded(onError = { TagWriteResult.Failed(it) }) {
            val propertyMap = HashMap<String, Array<String>>()
            tags.toMap().forEach { (key, values) -> propertyMap[key] = values.toTypedArray() }
            if (!TagLib.savePropertyMap(file.detachDuplicateFd(), propertyMap)) {
                return@guarded TagWriteResult.Failed("TagLib could not save the tags")
            }
            if (
                pictures != null &&
                    !TagLib.savePictures(
                        file.detachDuplicateFd(),
                        pictures.map { it.toTagLibPicture() }.toTypedArray(),
                    )
            ) {
                return@guarded TagWriteResult.Failed("TagLib could not save the pictures")
            }
            TagWriteResult.Written
        }

    /** Native code throws whatever it likes; none of it may crash the app. */
    private inline fun <T> guarded(onError: (String) -> T, block: () -> T): T =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            onError(error.message ?: error::class.java.simpleName)
        }
}

internal fun Map<String, Array<String>>.toTagMap(): TagMap =
    TagMap.of(mapValues { (_, values) -> values.toList() })

internal fun Picture.toEmbeddedPicture() =
    EmbeddedPicture(
        data = data,
        mimeType = mimeType.ifBlank { "image/jpeg" },
        description = description,
        type = PictureType.parse(pictureType),
    )

internal fun EmbeddedPicture.toTagLibPicture() =
    Picture(data = data, description = description, pictureType = type.label, mimeType = mimeType)
