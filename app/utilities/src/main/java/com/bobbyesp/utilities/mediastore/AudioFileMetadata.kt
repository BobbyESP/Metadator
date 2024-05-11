package com.bobbyesp.utilities.mediastore

import com.bobbyesp.ext.PropertyMap
import kotlinx.serialization.Serializable

@Serializable
data class AudioFileMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val trackNumber: String?,
    val discNumber: String?,
    val date: String?,
    val genre: String?,
    val composer: String?,
    val lyricist: String?,
    val performer: String?,
    val conductor: String?,
    val remixer: String?,
    val comment: String?,
) {
    companion object {
        fun AudioFileMetadata.toPropertyMap(): PropertyMap {
            return hashMapOf(
                "TITLE" to arrayOf(title ?: ""),
                "ARTIST" to arrayOf(artist ?: ""),
                "ALBUM" to arrayOf(album ?: ""),
                "ALBUMARTIST" to arrayOf(albumArtist ?: ""),
                "TRACKNUMBER" to arrayOf(trackNumber ?: ""),
                "DISCNUMBER" to arrayOf(discNumber ?: ""),
                "DATE" to arrayOf(date ?: ""),
                "GENRE" to arrayOf(genre ?: ""),
                "COMPOSER" to arrayOf(composer ?: ""),
                "LYRICIST" to arrayOf(lyricist ?: ""),
                "CONDUCTOR" to arrayOf(conductor ?: ""),
                "PERFORMER" to arrayOf(performer ?: ""),
                "REMIXER" to arrayOf(remixer ?: ""),
                "COMMENT" to arrayOf(comment ?: ""),
            )
        }

        fun Map<String, String?>.toAudioFileMetadata(): AudioFileMetadata {
            return AudioFileMetadata(
                title = this["TITLE"],
                artist = this["ARTIST"],
                album = this["ALBUM"],
                albumArtist = this["ALBUMARTIST"],
                trackNumber = this["TRACKNUMBER"],
                discNumber = this["DISCNUMBER"],
                date = this["DATE"],
                genre = this["GENRE"],
                composer = this["COMPOSER"],
                lyricist = this["LYRICIST"],
                performer = this["PERFORMER"],
                conductor = this["CONDUCTOR"],
                remixer = this["REMIXER"],
                comment = this["COMMENT"],
            )
        }
    }
}
