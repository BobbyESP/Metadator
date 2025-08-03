package com.bobbyesp.mediaplayer.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.bobbyesp.mediaplayer.domain.enums.AudioChannels
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
data class TrackTechnicalDetails(
    val bitrate: Int,
    val sampleRate: Int,
    val channels: AudioChannels,
) : Parcelable
