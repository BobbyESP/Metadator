package com.bobbyesp.utilities.mediastore

import androidx.compose.runtime.Stable
import com.bobbyesp.utilities.ext.PropertyMap
import com.bobbyesp.utilities.ext.formatForField
import kotlinx.serialization.Serializable

@Serializable
@Stable
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
    val sylt: String?,
    val uslt: String?
) {
    companion object {
        fun AudioFileMetadata.toPropertyMap(): PropertyMap {
            return hashMapOf(
                "TITLE" to arrayOf(title ?: ""),
                "ARTIST" to artist.formatForField(),
                "ALBUM" to arrayOf(album ?: ""),
                "ALBUMARTIST" to albumArtist.formatForField(),
                "TRACKNUMBER" to arrayOf(trackNumber ?: ""),
                "DISCNUMBER" to arrayOf(discNumber ?: ""),
                "DATE" to arrayOf(date ?: ""),
                "GENRE" to genre.formatForField(),
                "COMPOSER" to composer.formatForField(),
                "LYRICIST" to lyricist.formatForField(),
                "CONDUCTOR" to conductor.formatForField(),
                "PERFORMER" to performer.formatForField(),
                "REMIXER" to remixer.formatForField(),
                "COMMENT" to arrayOf(comment ?: ""),
                "SYLT" to arrayOf(sylt ?: ""),
                "USLT" to arrayOf(uslt ?: ""),
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
                sylt = this["SYLT"],
                uslt = this["USLT"],
            )
        }
    }
}
