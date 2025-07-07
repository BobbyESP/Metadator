package com.bobbyesp.metadator.tageditor.domain

import kotlinx.serialization.Serializable

@Serializable
data class AudioEditableMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val date: String = "",
    val genre: String = "",
    val comment: String = "",
    val lyrics: String = "",
)
