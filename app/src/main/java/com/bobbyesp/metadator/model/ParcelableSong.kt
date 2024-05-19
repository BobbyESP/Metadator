package com.bobbyesp.metadator.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.bobbyesp.model.UriSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Immutable
data class ParcelableSong(
    val name: String,
    val mainArtist: String,
    val localSongPath: String,
    @Serializable(with = UriSerializer::class) val artworkPath: Uri? = null,
    val fileName: String
) : Parcelable