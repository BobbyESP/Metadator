package com.bobbyesp.mediaplayer.domain.repository

import android.content.Context
import com.bobbyesp.mediaplayer.domain.model.MusicTrack

interface MediaStoreFolderScanner {
    val allowedExtensions: Set<String>
        get() = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus")

    suspend fun scanCustomFolder(path: String): List<MusicTrack>

    fun forceMediaStoreFolderScanning(context: Context, path: String)
}
