package com.bobbyesp.mediaplayer.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import com.bobbyesp.mediaplayer.domain.repository.MediaStoreFolderScanner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreFolderScannerImpl : MediaStoreFolderScanner {
    override suspend fun scanCustomFolder(path: String): List<MusicTrack> =
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.exists() || !folder.isDirectory) return@withContext emptyList()

            return@withContext folder
                .walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in allowedExtensions }
                .map { file ->
                    MusicTrack(
                        id = file.hashCode().toLong(),
                        title = file.nameWithoutExtension,
                        path = file.absolutePath,
                    )
                }
                .toList()
        }

    override fun forceMediaStoreFolderScanning(context: Context, path: String) {
        val file = File(path)
        if (!file.exists() || !file.isDirectory)
            throw IllegalArgumentException(
                "An invalid path was provided to be scanned by MediaStore"
            )

        val paths =
            file.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toList().toTypedArray()

        if (paths.isNotEmpty()) {
            MediaScannerConnection.scanFile(context, paths, arrayOf("audio/*")) { audioPath, uri ->
                println("Scanned: $audioPath, URI: $uri")
            }
        }
    }
}
