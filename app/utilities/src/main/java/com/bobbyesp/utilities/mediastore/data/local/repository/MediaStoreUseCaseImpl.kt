package com.bobbyesp.utilities.mediastore.data.local.repository

import android.content.ContentUris
import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.bobbyesp.utilities.mediastore.model.FileDescriptorMode
import com.bobbyesp.utilities.mediastore.model.repository.MediaStoreUseCase
import java.io.FileNotFoundException

class MediaStoreUseCaseImpl(private val context: Context) : MediaStoreUseCase {

    override fun getFileDescriptorFromPath(
        filePath: String,
        mode: FileDescriptorMode,
    ): ParcelFileDescriptor? {
        val resolver = context.contentResolver
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        resolver
            .query(
                audioUri,
                arrayOf(MediaStore.Files.FileColumns._ID),
                selection,
                selectionArgs,
                null,
            )
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileId =
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        )
                    val fileUri = ContentUris.withAppendedId(audioUri, fileId.toLong())
                    return try {
                        resolver.openFileDescriptor(fileUri, mode.modeKey)
                    } catch (e: FileNotFoundException) {
                        Log.e("MediaStoreHelper", "File not found: ${e.message}")
                        null
                    }
                }
                cursor.close()
            }
        return null
    }
}
