package com.bobbyesp.metadator.tags.api

/**
 * The fields the editor knows by name. Anything else in a file is still read, kept and editable
 * as a raw key under "All tags"; this list only decides what gets a labelled input.
 */
enum class TagField(
    val key: String,
    val multiValue: Boolean = false,
    val kind: FieldKind = FieldKind.Text,
) {
    Title("TITLE"),
    Artist("ARTIST", multiValue = true),
    Album("ALBUM"),
    AlbumArtist("ALBUMARTIST", multiValue = true),
    Date("DATE", kind = FieldKind.Date),
    Genre("GENRE", multiValue = true),
    TrackNumber("TRACKNUMBER", kind = FieldKind.Position),
    DiscNumber("DISCNUMBER", kind = FieldKind.Position),
    Composer("COMPOSER", multiValue = true),
    Lyricist("LYRICIST", multiValue = true),
    Conductor("CONDUCTOR", multiValue = true),
    Remixer("REMIXER", multiValue = true),
    Performer("PERFORMER", multiValue = true),
    Comment("COMMENT", kind = FieldKind.LongText),
    Lyrics("LYRICS", kind = FieldKind.LongText),
    Bpm("BPM", kind = FieldKind.Number),
    Isrc("ISRC"),
    Copyright("COPYRIGHT"),
    Label("LABEL"),
    Grouping("GROUPING");

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun forKey(key: String): TagField? = byKey[TagMap.normalizeKey(key)]

        /** The keys a track or disc total can be stored under besides "n/total". */
        val TrackTotalKeys = listOf("TRACKTOTAL", "TOTALTRACKS")
        val DiscTotalKeys = listOf("DISCTOTAL", "TOTALDISCS")
    }
}

enum class FieldKind {
    Text,
    LongText,
    Number,
    Date,
    /** A position and an optional total: track 3 of 12. */
    Position,
}
