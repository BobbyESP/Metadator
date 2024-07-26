package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.utilities.ext.toModifiableMap
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toAudioFileMetadata
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toPropertyMap
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ID3MetadataEditorPageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutablePageViewState = MutableStateFlow(PageViewState())
    val pageViewState = mutablePageViewState.asStateFlow()

    data class PageViewState(
        val metadata: Metadata? = null,
        val audioProperties: AudioProperties? = null,
        val state: ID3MetadataEditorPageState = ID3MetadataEditorPageState.Loading,
    )

    suspend fun loadTrackMetadata(path: String) {
        updateState(ID3MetadataEditorPageState.Loading)
        runCatching {
            MediaStoreReceiver.getFileDescriptorFromPath(context, path, mode = "r")?.use { songFd ->
                val fd = songFd.dup()?.detachFd()
                    ?: throw IllegalStateException("File descriptor is null")

                val metadata =
                    withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                        async {
                            TagLib.getMetadata(
                                fd = fd,
                            )
                        }.await()
                    } ?: throw IllegalStateException("Metadata is null")

                val fd2 = songFd.dup()?.detachFd()
                    ?: throw IllegalStateException("File descriptor is null")
                val audioProperties =
                    withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                        async {
                            TagLib.getAudioProperties(
                                fd = fd2,
                                readStyle = AudioPropertiesReadStyle.Fast
                            )
                        }.await()
                    } ?: throw IllegalStateException("Audio properties are null")

                updateStateMetadata(metadata, audioProperties)

                updateState(ID3MetadataEditorPageState.Success)
            }
        }.onFailure { error ->
            Log.e(
                "ID3MetadataEditorPageViewModel",
                "Error while trying to load metadata: ${error.message}"
            )
            updateState(ID3MetadataEditorPageState.Error(error))
        }
    }

    fun saveMetadata(
        context: Context = this.context,
        newMetadata: Metadata,
        path: String,
        imageUri: Uri?,
        intentPassthrough: (PendingIntent) -> Unit = {}
    ): Boolean {
        return try {
            val fd = MediaStoreReceiver.getFileDescriptorFromPath(context, path, mode = "w")
                ?: throw IOException("File descriptor is null")

            viewModelScope.launch(Dispatchers.IO) {
                fd.dup()?.detachFd()?.let {
                    TagLib.savePropertyMap(
                        it,
                        propertyMap = newMetadata.propertyMap
                    )
                }

                imageUri?.let {
                    savePicture(context, it, fd.detachFd())
                }
            }
            true
        } catch (securityException: SecurityException) {
            handleSecurityException(securityException, intentPassthrough)
            false
        } catch (e: IOException) {
            Log.e(
                "ID3MetadataEditorPageViewModel",
                "Error while trying to save metadata: ${e.message}"
            )
            updateState(ID3MetadataEditorPageState.Error(e))
            false
        }
    }

    private fun handleSecurityException(
        securityException: SecurityException,
        intentPassthrough: (PendingIntent) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val recoverableSecurityException = securityException as? RecoverableSecurityException
                ?: throw RuntimeException(securityException.message, securityException)

            intentPassthrough(recoverableSecurityException.userAction.actionIntent)
        } else {
            throw RuntimeException(securityException.message, securityException)
        }
    }

    private fun savePicture(context: Context, imageUri: Uri, fileDescriptorId: Int) {
        val byteArray = context.contentResolver.openInputStream(imageUri)?.readBytes() ?: return
        val mimeType = context.contentResolver.getType(imageUri) ?: return
        val picture = Picture(
            data = byteArray,
            mimeType = mimeType,
            description = "Song cover - Metadator",
            pictureType = "Cover (front)"
        )
        viewModelScope.launch(Dispatchers.IO) {
            TagLib.savePictures(
                fileDescriptorId,
                pictures = arrayOf(picture)
            )
        }
    }

    fun updateStatePropertyMap(propertyMap: Map<String, String>) {
        val mutableStateMap = mutablePageViewState.value.metadata?.propertyMap?.toModifiableMap()
        val updatedPropertyMap = mutableStateMap?.apply {
            putAll(propertyMap)
        } ?: propertyMap

        mutablePageViewState.update {
            it.copy(
                metadata = it.metadata?.copy(
                    propertyMap = updatedPropertyMap.toAudioFileMetadata().toPropertyMap()
                )
            )
        }
    }
    private fun updateState(state: ID3MetadataEditorPageState) {
        mutablePageViewState.update {
            it.copy(
                state = state
            )
        }
    }

    private fun updateStateMetadata(
        metadata: Metadata? = null,
        audioProperties: AudioProperties? = null
    ) {
        mutablePageViewState.update {
            it.copy(
                metadata = metadata,
                audioProperties = audioProperties
            )
        }
    }

    companion object {
        sealed class ID3MetadataEditorPageState {
            data object Loading : ID3MetadataEditorPageState()
            data object Success : ID3MetadataEditorPageState()
            data class Error(val throwable: Throwable) : ID3MetadataEditorPageState()
        }
    }
}