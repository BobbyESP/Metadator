package com.bobbyesp.mediaplayer.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Immutable @Serializable @Parcelize data class Genre(val id: Long, val name: String) : Parcelable
