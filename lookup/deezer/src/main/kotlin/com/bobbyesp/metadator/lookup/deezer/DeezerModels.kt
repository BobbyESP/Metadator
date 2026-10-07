package com.bobbyesp.metadator.lookup.deezer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeezerSearch(val data: List<DeezerTrack> = emptyList(), val error: DeezerError? = null)

@Serializable
internal data class DeezerTrack(
    val id: Long,
    val title: String,
    @SerialName("title_short") val titleShort: String? = null,
    val duration: Long? = null,
    val isrc: String? = null,
    @SerialName("track_position") val trackPosition: Int? = null,
    @SerialName("disk_number") val diskNumber: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val artist: DeezerArtist? = null,
    val contributors: List<DeezerArtist> = emptyList(),
    val album: DeezerAlbum? = null,
    val error: DeezerError? = null,
)

@Serializable internal data class DeezerArtist(val id: Long? = null, val name: String, val role: String? = null)

@Serializable
internal data class DeezerAlbum(
    val id: Long,
    val title: String,
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
)

@Serializable internal data class DeezerError(val type: String? = null, val message: String? = null, val code: Int? = null)
