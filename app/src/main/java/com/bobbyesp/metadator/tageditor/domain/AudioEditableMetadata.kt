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
) {
    companion object {
        fun fromMap(map: Map<String, String>): AudioEditableMetadata {
            return AudioEditableMetadata(
                title = map["TITLE"] ?: "",
                artist = map["ARTIST"] ?: "",
                album = map["ALBUM"] ?: "",
                trackNumber = map["TRACKNUMBER"]?.toIntOrNull() ?: 0,
                discNumber = map["DISCNUMBER"]?.toIntOrNull() ?: 0,
                date = map["DATE"] ?: "",
                genre = map["GENRE"] ?: "",
                comment = map["COMMENT"] ?: "",
                lyrics = map["LYRICS"] ?: ""
            )
        }
    }

    fun toMap(): Map<String, String> = mapOf(
        "TITLE" to title,
        "ARTIST" to artist,
        "ALBUM" to album,
        "TRACKNUMBER" to trackNumber.toString(),
        "DISCNUMBER" to discNumber.toString(),
        "DATE" to date,
        "GENRE" to genre,
        "COMMENT" to comment,
        "LYRICS" to lyrics,
    )
}
