package com.bobbyesp.metadator.model

import android.net.Uri
import android.os.Parcelable
import com.bobbyesp.model.UriSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SelectedSong(
    val name: String,
    val mainArtist: String,
    val localSongPath: String? = null,
    @Serializable(with = UriSerializer::class) val artworkPath: Uri? = null,
    val fileName: String? = null
) : Parcelable