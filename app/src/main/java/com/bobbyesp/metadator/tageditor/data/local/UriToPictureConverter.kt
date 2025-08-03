package com.bobbyesp.metadator.tageditor.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bobbyesp.metadator.tageditor.model.PictureType
import com.kyant.taglib.Picture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UriToPictureConverter(private val context: Context) {
    /**
     * Converts a list of URIs into Picture objects.
     *
     * @param uris List of URIs to be converted.
     * @param pictureType Type of image according to ID3v2 specification.
     * @param descriptionFormat Format string for custom descriptions (use %d for index).
     * @return List of Picture objects.
     */
    suspend fun convert(
        uris: List<Uri>,
        pictureType: PictureType = PictureType.FRONT_COVER,
        descriptionFormat: String = "Audio image %d - Metadator",
    ): List<Picture> =
        withContext(Dispatchers.IO) {
            uris.mapIndexedNotNull { index, uri ->
                try {
                    val byteArray =
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: run {
                                Log.e(TAG, "Failed to read image data: $uri")
                                return@mapIndexedNotNull null
                            }

                    val mimeType =
                        context.contentResolver.getType(uri)
                            ?: run {
                                Log.e(TAG, "Could not determine MIME type: $uri")
                                return@mapIndexedNotNull null
                            }

                    Picture(
                        data = byteArray,
                        mimeType = mimeType,
                        description = descriptionFormat.format(index + 1),
                        pictureType = pictureType.id3Type,
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image: $uri", e)
                    null
                }
            }
        }

    /**
     * Converts a single URI into a Picture object.
     *
     * @param uri The URI to be converted.
     * @param pictureType Image type as per ID3v2 specification.
     * @param description Description of the image.
     * @return A Picture object, or null if the conversion fails.
     */
    suspend fun convertSingle(
        uri: Uri,
        pictureType: PictureType = PictureType.FRONT_COVER,
        description: String = "Audio image - Metadator",
    ): Picture? {
        return convert(listOf(uri), pictureType, description).firstOrNull()
    }

    companion object {
        private const val TAG = "UriToPictureConverter"
    }
}
