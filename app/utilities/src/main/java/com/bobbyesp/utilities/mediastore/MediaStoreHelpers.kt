package com.bobbyesp.utilities.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.FileNotFoundException

//Todo: Move this to an interface
object MediaStoreHelpers {

    private val audioUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    fun getFileDescriptorFromPath(
        context: Context, filePath: String, mode: String = "r"
    ): ParcelFileDescriptor? {
        val resolver = context.contentResolver
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        resolver.query(
            audioUri, arrayOf(MediaStore.Files.FileColumns._ID), selection, selectionArgs, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val fileUri = ContentUris.withAppendedId(audioUri, fileId.toLong())
                return try {
                    resolver.openFileDescriptor(fileUri, mode)
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