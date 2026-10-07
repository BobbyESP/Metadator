package com.bobbyesp.metadator.tags.api

/** A picture stored in the file. Compared by content, not by array identity. */
class EmbeddedPicture(
    val data: ByteArray,
    val mimeType: String,
    val description: String = "",
    val type: PictureType = PictureType.FrontCover,
) {
    val sizeBytes: Int
        get() = data.size

    fun copy(
        data: ByteArray = this.data,
        mimeType: String = this.mimeType,
        description: String = this.description,
        type: PictureType = this.type,
    ) = EmbeddedPicture(data, mimeType, description, type)

    override fun equals(other: Any?): Boolean =
        other is EmbeddedPicture &&
            mimeType == other.mimeType &&
            description == other.description &&
            type == other.type &&
            data.contentEquals(other.data)

    override fun hashCode(): Int =
        ((data.contentHashCode() * 31 + mimeType.hashCode()) * 31 + description.hashCode()) * 31 +
            type.hashCode()

    override fun toString(): String =
        "EmbeddedPicture(type=$type, mimeType=$mimeType, size=${data.size})"
}

/** The ID3v2 picture types, which FLAC and Ogg reuse. MP4 only has untyped covers. */
enum class PictureType(val label: String) {
    Other("Other"),
    FileIcon("File Icon"),
    OtherFileIcon("Other File Icon"),
    FrontCover("Front Cover"),
    BackCover("Back Cover"),
    LeafletPage("Leaflet Page"),
    Media("Media"),
    LeadArtist("Lead Artist"),
    Artist("Artist"),
    Conductor("Conductor"),
    Band("Band"),
    Composer("Composer"),
    Lyricist("Lyricist"),
    RecordingLocation("Recording Location"),
    DuringRecording("During Recording"),
    DuringPerformance("During Performance"),
    MovieScreenCapture("Movie Screen Capture"),
    ColouredFish("Coloured Fish"),
    Illustration("Illustration"),
    BandLogo("Band Logo"),
    PublisherLogo("Publisher Logo");

    companion object {
        /** Lenient: TagLib's spelling varies by format ("Front Cover", "front cover", "FrontCover"). */
        fun parse(value: String?): PictureType {
            val wanted = value.orEmpty().filter { it.isLetter() }.lowercase()
            return entries.firstOrNull { it.label.filter(Char::isLetter).lowercase() == wanted }
                ?: if (wanted.isEmpty()) FrontCover else Other
        }
    }
}
