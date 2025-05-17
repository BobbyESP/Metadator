package com.bobbyesp.metadator.tageditor.data.local.repository

import com.bobbyesp.metadator.tageditor.model.repository.AudioMetadataRepository
import com.bobbyesp.utilities.mediastore.model.FileDescriptorMode
import com.bobbyesp.utilities.mediastore.model.repository.MediaStoreUseCase
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioMetadataRepositoryImpl(private val mediaStoreUseCase: MediaStoreUseCase) :
    AudioMetadataRepository {

    /** Retrieves a detached file descriptor from the given path and mode. */
    private fun getDetachedFileDescriptor(path: String, mode: FileDescriptorMode): Int? {
        return mediaStoreUseCase.getFileDescriptorFromPath(path, mode)?.use { fd ->
            fd.dup()?.detachFd()
        }
    }

    override suspend fun getMetadata(path: String): Result<Metadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                getDetachedFileDescriptor(path, FileDescriptorMode.READ)?.let {
                    TagLib.getMetadata(it)
                } ?: throw IllegalStateException("Failed to retrieve metadata")
            }
        }

    override suspend fun getAudioProperties(
        path: String,
        style: AudioPropertiesReadStyle,
    ): Result<AudioProperties> =
        withContext(Dispatchers.IO) {
            runCatching {
                getDetachedFileDescriptor(path, FileDescriptorMode.READ)?.let {
                    TagLib.getAudioProperties(it, style)
                } ?: throw IllegalStateException("Failed to retrieve audio properties")
            }
        }

    override suspend fun writePropertyMap(path: String, propertyMap: PropertyMap): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                getDetachedFileDescriptor(path, FileDescriptorMode.WRITE)?.let {
                    TagLib.savePropertyMap(it, propertyMap)
                } ?: throw IllegalStateException("Failed to write property map")
            }
        }

    override suspend fun writePictures(path: String, pictures: List<Picture>): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                getDetachedFileDescriptor(path, FileDescriptorMode.WRITE)?.let {
                    TagLib.savePictures(it, pictures.toTypedArray())
                } ?: throw IllegalStateException("Failed to write pictures")
            }
        }
}
