package com.bobbyesp.utilities.mediastore.model.repository

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.bobbyesp.utilities.mediastore.model.FileDescriptorMode

interface MediaStoreUseCase {
    fun getFileDescriptorFromPath(
        filePath: String,
        mode: FileDescriptorMode,
    ): ParcelFileDescriptor?

    val audioUri: Uri
        get() = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
}