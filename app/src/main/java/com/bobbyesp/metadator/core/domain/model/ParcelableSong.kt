package com.bobbyesp.metadator.core.domain.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import com.bobbyesp.utilities.mediastore.model.UriSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import androidx.core.net.toUri

@Parcelize
@Immutable
@Serializable
data class ParcelableSong(
    val name: String,
    val mainArtist: String,
    val localPath: String,
    @Serializable(with = UriSerializer::class) val artworkPath: Uri? = null,
    val filename: String
) : Parcelable {
    fun MusicTrack.toParcelableSong(): ParcelableSong {
        return ParcelableSong(
            name = title,
            mainArtist = artist ?: "",
            localPath = path,
            artworkPath = artworkUri?.toUri(),
            filename = title
        )
    }
}