package com.bobbyesp.ext

import com.bobbyesp.utilities.mediastore.AudioFileMetadata

typealias PropertyMap = HashMap<String, Array<String>>

fun PropertyMap.toAudioFileMetadata(): AudioFileMetadata {
    return AudioFileMetadata(
        title = this["TITLE"]?.getOrNull(0),
        artist = this["ARTIST"]?.getOrNull(0),
        album = this["ALBUM"]?.getOrNull(0),
        albumArtist = this["ALBUMARTIST"]?.getOrNull(0),
        trackNumber = this["TRACKNUMBER"]?.getOrNull(0),
        discNumber = this["DISCNUMBER"]?.getOrNull(0),
        date = this["DATE"]?.getOrNull(0),
        genre = this["GENRE"]?.getOrNull(0),
        composer = this["COMPOSER"]?.getOrNull(0),
        lyricist = this["LYRICIST"]?.getOrNull(0),
        performer = this["PERFORMER"]?.getOrNull(0),
        conductor = this["CONDUCTOR"]?.getOrNull(0),
        remixer = this["REMIXER"]?.getOrNull(0),
        comment = this["COMMENT"]?.getOrNull(0),
    )
}

fun PropertyMap.toModifiableMap(): MutableMap<String, String?> {
    return mutableMapOf(
        "TITLE" to this["TITLE"]?.getOrNull(0),
        "ARTIST" to this["ARTIST"]?.getOrNull(0),
        "ALBUM" to this["ALBUM"]?.getOrNull(0),
        "ALBUMARTIST" to this["ALBUMARTIST"]?.getOrNull(0),
        "TRACKNUMBER" to this["TRACKNUMBER"]?.getOrNull(0),
        "DISCNUMBER" to this["DISCNUMBER"]?.getOrNull(0),
        "DATE" to this["DATE"]?.getOrNull(0),
        "GENRE" to this["GENRE"]?.getOrNull(0),
        "COMPOSER" to this["COMPOSER"]?.getOrNull(0),
        "LYRICIST" to this["LYRICIST"]?.getOrNull(0),
        "PERFORMER" to this["PERFORMER"]?.getOrNull(0),
        "CONDUCTOR" to this["CONDUCTOR"]?.getOrNull(0),
        "REMIXER" to this["REMIXER"]?.getOrNull(0),
        "COMMENT" to this["COMMENT"]?.getOrNull(0),
    )
}