package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.utilities.ext.toModifiableMap
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toAudioFileMetadata
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toPropertyMap
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.states.ScreenState
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MetadataEditorVM @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PageViewState())
    val state = mutableState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var latestLoadedSongPath: String? = null

    data class PageViewState(
        val metadata: ResourceState<Metadata?> = ResourceState.Loading(),
        val audioProperties: ResourceState<AudioProperties?> = ResourceState.Loading(),
        val pageState: ScreenState<Nothing> = ScreenState.Loading,
        val mutablePropertiesMap: SnapshotStateMap<String, String> = mutableStateMapOf()
    )

    private suspend fun loadTrackMetadata(path: String) {
        updateState(ScreenState.Loading)
        mutableState.value.mutablePropertiesMap.clear()
        runCatching {
            MediaStoreReceiver.getFileDescriptorFromPath(context, path, mode = "r")?.use { songFd ->

                val metadata = loadAudioMetadata(songFd)

                val audioProperties = loadAudioProperties(songFd)

                updateAudioInformation(metadata, audioProperties)
            }
        }.onFailure { error ->
            Log.e(
                "MetadataEditorVM", "Error while trying to load the audio file: ${error.message}"
            )
            when (error) {
                is NullAudioFileDescriptorException -> {
                    if (error.isAudioProperties) mutableState.update {
                        it.copy(
                            audioProperties = ResourceState.Error(
                                message = error.message ?: error.stackTrace.toString()
                            )
                        )
                    } else {
                        mutableState.update {
                            it.copy(
                                metadata = ResourceState.Error(
                                    message = error.message ?: error.stackTrace.toString()
                                )
                            )
                        }
                    }
                }

                else -> {
                    updateState(ScreenState.Error(error))
                }
            }
            updateState(ScreenState.Error(error))
        }.onSuccess {
            updateState(ScreenState.Success(null))
            mutableState.value.metadata.data?.propertyMap?.toModifiableMap()?.forEach {
                mutableState.value.mutablePropertiesMap[it.key] = it.value ?: ""
            }
        }
    }

    private suspend fun loadAudioMetadata(songFd: ParcelFileDescriptor): Metadata? {
        val fd = songFd.dup()?.detachFd() ?: throw NullAudioFileDescriptorException(
            isAudioProperties = false
        )

        return withContext(Dispatchers.IO) {
            TagLib.getMetadata(fd = fd)
        }
    }


    private suspend fun loadAudioProperties(
        songFd: ParcelFileDescriptor,
        readStyle: AudioPropertiesReadStyle = AudioPropertiesReadStyle.Average
    ): AudioProperties? {
        val fd = songFd.dup()?.detachFd() ?: throw NullAudioFileDescriptorException(
            isAudioProperties = true
        )

        return withContext(Dispatchers.IO) {
            TagLib.getAudioProperties(
                fd = fd, readStyle = readStyle
            )
        }
    }

    private fun updateMapProperty(key: String, value: String) {
        mutableState.value.mutablePropertiesMap[key] = value
    }

    fun savePropertyMap(
        context: Context = this.context,
        newPropertiesMap: PropertyMap = mutableState.value.mutablePropertiesMap.toAudioFileMetadata()
            .toPropertyMap(),
        audioPath: String,
        intentPassthrough: (PendingIntent) -> Unit = {}
    ): Boolean {
        return try {
            val fd = MediaStoreReceiver.getFileDescriptorFromPath(context, audioPath, mode = "w")
                ?: throw NullAudioFileDescriptorException(isAudioProperties = false)

            fd.dup().detachFd().let {
                TagLib.savePropertyMap(
                    it, propertyMap = newPropertiesMap
                )
            }

            true
        } catch (securityException: SecurityException) {
            handleSecurityException(securityException, intentPassthrough)
            false
        } catch (e: Exception) {
            mutableState.update {
                it.copy(
                    pageState = ScreenState.Error(e)
                )
            }
            false
        }
    }

    private fun savePictures(
        context: Context = this.context,
        imagesUri: List<Uri> = emptyList(),
        audioPath: String,
        intentPassthrough: (PendingIntent) -> Unit
    ): Boolean {
        return try {
            val fd = MediaStoreReceiver.getFileDescriptorFromPath(context, audioPath, mode = "w")
                ?: throw NullAudioFileDescriptorException(isAudioProperties = false)

            val mutablePicturesList = mutableListOf<Picture>()
            fd.dup().detachFd().let {
                imagesUri.forEachIndexed { index, imageUri ->
                    val byteArray = context.contentResolver.openInputStream(imageUri)?.readBytes()
                        ?: return@forEachIndexed
                    val mimeType =
                        context.contentResolver.getType(imageUri) ?: return@forEachIndexed
                    val picture = Picture(
                        data = byteArray,
                        mimeType = mimeType,
                        description = "Audio image $index - Metadator",
                        pictureType = "Front cover"
                    )

                    mutablePicturesList.add(picture)
                }

                TagLib.savePictures(
                    it, pictures = mutablePicturesList.toTypedArray()
                )
            }
            true
        } catch (securityException: SecurityException) {
            handleSecurityException(securityException, intentPassthrough)
            false
        } catch (e: Exception) {
            mutableState.update {
                it.copy(
                    pageState = ScreenState.Error(e)
                )
            }
            false
        }
    }

    private fun savePictures(
        context: Context = this.context,
        imagesUri: List<Uri> = emptyList(),
        fileDescriptorId: Int = -1
    ): Boolean {

        val mutablePicturesList = mutableListOf<Picture>()

        imagesUri.forEachIndexed { index, uri ->
            val byteArray = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: throw IllegalStateException("Image byte array is null")
            val mimeType = context.contentResolver.getType(uri)
                ?: throw IllegalStateException("Image mime type is null")

            val picture = Picture(
                data = byteArray,
                mimeType = mimeType,
                description = "Audio image $index - Metadator",
                pictureType = "Front cover"
            )

            mutablePicturesList.add(picture)
        }

        kotlin.runCatching {
            TagLib.savePictures(
                fileDescriptorId, pictures = mutablePicturesList.toTypedArray()
            )
        }.onFailure {
            return false
        }.onSuccess {
            return true
        }

        return true
    }

    private fun handleSecurityException(
        securityException: SecurityException, intentPassthrough: (PendingIntent) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val recoverableSecurityException =
                securityException as? RecoverableSecurityException ?: throw RuntimeException(
                    securityException.message, securityException
                )

            intentPassthrough(recoverableSecurityException.userAction.actionIntent)
        } else {
            throw RuntimeException(securityException.message, securityException)
        }
    }


    private fun updateAudioInformation(metadata: Metadata?, audioProperties: AudioProperties?) {
        mutableState.update {
            it.copy(
                metadata = ResourceState.Success(metadata),
                audioProperties = ResourceState.Success(audioProperties)
            )
        }
    }

    private fun updateState(state: ScreenState<Nothing>) {
        mutableState.update {
            it.copy(
                pageState = state
            )
        }
    }

    private fun emitUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventFlow.emit(event)
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadMetadata -> {
                viewModelScope.launch {
                    if (latestLoadedSongPath != event.path) {
                        loadTrackMetadata(event.path)
                        latestLoadedSongPath = event.path
                    }
                }
            }

            is Event.SaveProperties -> {
                val succeeded = savePropertyMap(
                    audioPath = event.path,
                    intentPassthrough = {
                        emitUiEvent(UiEvent.RequestPermission(it))
                    }
                )

                if (succeeded) {
                    emitUiEvent(UiEvent.SaveSuccess(properties = true))
                } else {
                    emitUiEvent(UiEvent.SaveFailed)
                }
            }

            is Event.SavePictures -> {
                val succeeded = savePictures(
                    imagesUri = event.imagesUri,
                    audioPath = event.path,
                    intentPassthrough = {
                        emitUiEvent(UiEvent.RequestPermission(it))
                    })

                if (succeeded) {
                    emitUiEvent(UiEvent.SaveSuccess(pictures = true))
                } else {
                    emitUiEvent(UiEvent.SaveFailed)
                }
            }

            is Event.SaveAll -> {
                val propertiesSaved = savePropertyMap(audioPath = event.path, intentPassthrough = {
                    emitUiEvent(UiEvent.RequestPermission(it))
                })

                val succeededPictures = savePictures(
                    audioPath = event.path,
                    imagesUri = emptyList(),
                    intentPassthrough = {
                        emitUiEvent(UiEvent.RequestPermission(it))
                    })

                if (propertiesSaved || succeededPictures) {
                    emitUiEvent(
                        UiEvent.SaveSuccess(
                            pictures = succeededPictures, properties = propertiesSaved
                        )
                    )
                } else {
                    emitUiEvent(UiEvent.SaveFailed)
                }
            }

            is Event.UpdateProperty -> {
                Log.i(
                    "MetadataEditorVM",
                    "Received property ${event.key} with value ${event.value}"
                )
                updateMapProperty(event.key, event.value)
            }
        }
    }


    interface Event {
        data class LoadMetadata(val path: String) : Event
        data class SaveAll(val path: String) : Event
        data class SaveProperties(val path: String) : Event
        data class SavePictures(val path: String, val imagesUri: List<Uri>) : Event
        data class UpdateProperty(val key: String, val value: String) : Event
    }

    interface UiEvent {
        data class RequestPermission(val intent: PendingIntent) : UiEvent
        data class SaveSuccess(val pictures: Boolean? = null, val properties: Boolean? = null) :
            UiEvent

        data object SaveFailed : UiEvent
    }

    companion object {
        class NullAudioFileDescriptorException(val isAudioProperties: Boolean) :
            Exception("Audio file descriptor is null")
    }
}