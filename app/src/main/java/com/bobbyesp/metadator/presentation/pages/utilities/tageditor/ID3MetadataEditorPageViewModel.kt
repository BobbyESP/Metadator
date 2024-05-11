package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.features.spotify.domain.repositories.SearchRepository
import com.bobbyesp.utilities.mediastore.AudioFileMetadata
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toAudioFileMetadata
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
    private val searchRepository: SearchRepository
) : ViewModel() {
    private val mutablePageViewState = MutableStateFlow(PageViewState())
    val pageViewState = mutablePageViewState.asStateFlow()

    val propertiesCopy = mutableStateOf<AudioFileMetadata?>(null)

    data class PageViewState(
        val metadata: Metadata? = null,
        val audioFileMetadata: AudioFileMetadata? = null,
        val audioProperties: AudioProperties? = null,
        val state: ID3MetadataEditorPageState = ID3MetadataEditorPageState.Loading,
    )

    suspend fun loadTrackMetadata(path: String) {
        updateState(ID3MetadataEditorPageState.Loading)
        kotlin.runCatching {
            MediaStoreReceiver.getFileDescriptorFromPath(context, path, mode = "r")?.use { songFd ->
                val fd = songFd.dup()?.detachFd()
                    ?: throw IllegalStateException("File descriptor is null")

                val metadataDeferred =
                    withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                        async {
                            TagLib.getMetadata(
                                fd,
                                readStyle = AudioPropertiesReadStyle.Fast
                            )
                        }
                    }

                val metadata = metadataDeferred.await()

                if (metadata == null) {
                    updateState(ID3MetadataEditorPageState.Error(Exception("Metadata is null")))
                    return
                }

                updateMetadata(metadata)

                updateState(ID3MetadataEditorPageState.Success)

                propertiesCopy.value = metadata.propertyMap.toAudioFileMetadata()
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
        Log.i("ID3MetadataEditorPageViewModel", "Security exception caught")
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
        val saved = TagLib.savePictures(
            fileDescriptorId,
            pictures = arrayOf(picture)
        )

        if (saved) {
            Log.i("ID3MetadataEditorPageViewModel", "Saved picture")
        } else {
            Log.e("ID3MetadataEditorPageViewModel", "Error while trying to save picture")
        }
    }

    suspend fun getSpotifyResults(query: String): List<Track> {
        val result = searchRepository.searchTracks(query)
        return if (result.isSuccess) {
            result.getOrNull() ?: emptyList()
        } else {
            result.exceptionOrNull()?.printStackTrace()
            emptyList()
        }
    }

    private fun updateState(state: ID3MetadataEditorPageState) {
        mutablePageViewState.update {
            it.copy(
                state = state
            )
        }
    }

    private fun updateMetadata(metadata: Metadata? = null) {
        mutablePageViewState.update {
            it.copy(
                metadata = metadata,
                audioFileMetadata = metadata?.propertyMap?.toAudioFileMetadata(),
                audioProperties = metadata?.audioProperties
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