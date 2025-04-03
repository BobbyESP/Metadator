package com.bobbyesp.metadator.tageditor.presentation.pages.tageditor

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.util.executeIfDebugging
import com.bobbyesp.metadator.tageditor.data.local.UriToPictureConverter
import com.bobbyesp.metadator.tageditor.model.repository.AudioMetadataRepository
import com.bobbyesp.utilities.ext.toModifiableMap
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toAudioFileMetadata
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toPropertyMap
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.states.ScreenState
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MetadataEditorViewModel(
    private val repository: AudioMetadataRepository,
    private val uriConverter: UriToPictureConverter,
    private val stateHandle: SavedStateHandle,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val eventFlow = _eventFlow.asSharedFlow()

    private var latestLoadedSongPath: String? = null

    data class PageViewState(
        val metadata: ResourceState<Metadata?> = ResourceState.Loading(),
        val audioProperties: ResourceState<AudioProperties?> = ResourceState.Loading(),
        val pageState: ScreenState<Nothing> = ScreenState.Loading,
        val properties: Map<String, String> = emptyMap(), // Immutable properties
        val modifiedKeys: Set<String> = emptySet() //Which keys have been modified

    )

    private val _state = MutableStateFlow(PageViewState())
    val state =
        _state.onStart { stateHandle.get<String>("path")?.let { onEvent(Event.LoadMetadata(it)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PageViewState())

    override fun onCleared() {
        super.onCleared()
        updateState(ScreenState.Loading)
        _state.update {
            it.copy(metadata = ResourceState.Loading(), audioProperties = ResourceState.Loading())
        }
    }

    private suspend fun loadTrackMetadata(path: String, scope: CoroutineScope) {
        _state.update {
            it.copy(
                pageState = ScreenState.Loading, properties = emptyMap(), modifiedKeys = emptySet()
            )
        }

        try {
            stateHandle["path"] = path

            val metadataDeferred = scope.async {
                repository.getMetadata(path)
            }

            val audioPropertiesDeferred = scope.async {
                repository.getAudioProperties(path, AudioPropertiesReadStyle.Average)
            }

            val metadataResult = metadataDeferred.await()
            val audioPropertiesResult = audioPropertiesDeferred.await()

            metadataResult.fold(onSuccess = { metadata ->
                val propMap = metadata.propertyMap?.toModifiableMap()?.mapValues { it.value ?: "" }
                    ?: emptyMap()
                _state.update {
                    it.copy(
                        metadata = ResourceState.Success(metadata), properties = propMap
                    )
                }
            }, onFailure = { error ->
                _state.update {
                    it.copy(metadata = ResourceState.Error(error.message ?: error.toString()))
                }
            })

            audioPropertiesResult.fold(onSuccess = { audioProperties ->
                _state.update {
                    it.copy(audioProperties = ResourceState.Success(audioProperties))
                }
            }, onFailure = { error ->
                _state.update {
                    it.copy(
                        audioProperties = ResourceState.Error(
                            error.message ?: error.toString()
                        )
                    )
                }
            })

            updateState(ScreenState.Success(null))
        } catch (error: Exception) {
            Log.e("MetadataEditorVM", "Error while trying to load the audio file: ${error.message}")
            updateState(ScreenState.Error(error))
        }
    }

    private fun updateMapProperty(key: String, value: String) {
        _state.update { currentState ->
            val updatedProperties = currentState.properties.toMutableMap().apply {
                put(key, value)
            }
            val updatedModifiedKeys = currentState.modifiedKeys + key

            currentState.copy(
                properties = updatedProperties, modifiedKeys = updatedModifiedKeys
            )
        }
    }

    private suspend fun savePropertyMap(
        audioPath: String,
        intentPassthrough: (PendingIntent) -> Unit = {},
    ) {
        try {
            // Convert the actual properties to a PropertyMap
            val propertyMap = _state.value.properties.toAudioFileMetadata().toPropertyMap()

            repository.writePropertyMap(audioPath, propertyMap).onSuccess {
                // Having successfully saved the properties, we can clear the modified keys
                _state.update { it.copy(modifiedKeys = emptySet()) }
            }.onFailure { error ->
                if (error is SecurityException) {
                    handleSecurityException(error, intentPassthrough)
                } else {
                    _state.update { it.copy(pageState = ScreenState.Error(error)) }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(pageState = ScreenState.Error(e)) }
        }
    }

    private suspend fun savePictures(
        imagesUri: List<Uri>,
        audioPath: String,
        intentPassthrough: (PendingIntent) -> Unit,
    ) {
        try {
            val pictures = uriConverter.convert(imagesUri)
            repository.writePictures(audioPath, pictures).onFailure { error ->
                if (error is SecurityException) {
                    handleSecurityException(error, intentPassthrough)
                } else {
                    _state.update { it.copy(pageState = ScreenState.Error(error)) }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(pageState = ScreenState.Error(e)) }
        }
    }

    private fun handleSecurityException(
        securityException: SecurityException,
        intentPassthrough: (PendingIntent) -> Unit,
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

    private fun updateState(state: ScreenState<Nothing>) {
        _state.update { it.copy(pageState = state) }
    }

    private fun emitUiEvent(event: UiEvent) {
        viewModelScope.launch { _eventFlow.emit(event) }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadMetadata -> {
                viewModelScope.launch {
                    if (latestLoadedSongPath != event.path) {
                        loadTrackMetadata(event.path, this)
                        latestLoadedSongPath = event.path
                    }
                }
            }

            is Event.SaveProperties -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val succeeded = try {
                        savePropertyMap(
                            audioPath = event.path,
                            intentPassthrough = { emitUiEvent(UiEvent.RequestPermission(it)) }
                        )
                        true
                    } catch (e: Exception) {
                        executeIfDebugging {
                            Log.e(
                                "MetadataEditorVM",
                                "Error while trying to save the properties: ${e.message}"
                            )
                        }
                        emitUiEvent(UiEvent.SaveFailed)
                        false
                    }

                    if (succeeded) {
                        emitUiEvent(UiEvent.SaveSuccess(properties = true))
                    } else {
                        emitUiEvent(UiEvent.SaveFailed)
                    }
                }
            }

            is Event.SavePictures -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val succeeded = try {
                        savePictures(
                            audioPath = event.path,
                            imagesUri = event.imagesUri,
                            intentPassthrough = { emitUiEvent(UiEvent.RequestPermission(it)) }
                        )
                        true
                    } catch (e: Exception) {
                        executeIfDebugging {
                            Log.e(
                                "MetadataEditorVM",
                                "Error while trying to save the pictures: ${e.message}"
                            )
                        }
                        emitUiEvent(UiEvent.SaveFailed)
                        false
                    }

                    if (succeeded) {
                        emitUiEvent(UiEvent.SaveSuccess(pictures = true))
                    } else {
                        emitUiEvent(UiEvent.SaveFailed)
                    }
                }
            }

            is Event.SaveAll -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val succeeded = try {
                        savePropertyMap(
                            audioPath = event.path,
                            intentPassthrough = { emitUiEvent(UiEvent.RequestPermission(it)) }
                        )
                        savePictures(
                            audioPath = event.path,
                            imagesUri = event.imagesUri,
                            intentPassthrough = { emitUiEvent(UiEvent.RequestPermission(it)) }
                        )
                        true
                    } catch (e: Exception) {
                        executeIfDebugging {
                            Log.e(
                                "MetadataEditorVM",
                                "Error while trying to save all: ${e.message}"
                            )
                        }
                        emitUiEvent(UiEvent.SaveFailed)
                        false
                    }

                    if (succeeded) {
                        emitUiEvent(UiEvent.SaveSuccess(pictures = true, properties = true))
                    } else {
                        emitUiEvent(UiEvent.SaveFailed)
                    }
                }
            }

            is Event.UpdateProperty -> {
                updateMapProperty(event.key, event.value)
            }
        }
    }

    interface Event {
        data class LoadMetadata(val path: String) : Event
        data class SaveAll(val path: String, val imagesUri: List<Uri>) : Event
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
}