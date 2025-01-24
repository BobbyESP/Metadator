package com.bobbyesp.metadator.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GeneratingTokens
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RunningWithErrors
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Title
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.util.executeIfDebugging

object TagLib {
    @Composable
    fun String.toLocalizedName(): String {
        return when (this) {
            "TITLE" -> stringResource(id = R.string.title)
            "ARTIST" -> stringResource(id = R.string.artist)
            "ALBUM" -> stringResource(id = R.string.album)
            "ALBUMARTIST" -> stringResource(id = R.string.album_artist)
            "TRACKNUMBER" -> stringResource(id = R.string.track_number)
            "DISCNUMBER" -> stringResource(id = R.string.disc_number)
            "DATE" -> stringResource(id = R.string.date)
            "GENRE" -> stringResource(id = R.string.genre)
            "COMPOSER" -> stringResource(id = R.string.composer)
            "LYRICIST" -> stringResource(id = R.string.lyricist)
            "PERFORMER" -> stringResource(id = R.string.performer)
            "CONDUCTOR" -> stringResource(id = R.string.conductor)
            "REMIXER" -> stringResource(id = R.string.remixer)
            "COMMENT" -> stringResource(id = R.string.comment)
            else -> this
        }
    }

    @Composable
    fun String.toImageVector(): ImageVector {
        return when (this) {
            "TITLE" -> Icons.Rounded.Title
            "ARTIST" -> Icons.Rounded.Person
            "ALBUM" -> Icons.Rounded.Album
            "ALBUMARTIST" -> Icons.Rounded.Person
            "TRACKNUMBER" -> Icons.Rounded.Numbers
            "DISCNUMBER" -> Icons.Rounded.Numbers
            "DATE" -> Icons.Rounded.CalendarMonth
            "GENRE" -> Icons.Rounded.GeneratingTokens
            "COMPOSER" -> Icons.Rounded.Person
            "LYRICIST" -> Icons.Rounded.Person
            "PERFORMER" -> Icons.Rounded.Person
            "CONDUCTOR" -> Icons.Rounded.Person
            "REMIXER" -> Icons.Rounded.Person
            "COMMENT" -> Icons.Rounded.Subtitles
            else -> Icons.Rounded.RunningWithErrors
        }
    }
}

object StringPreferencesExtensions {

    inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
        if (this == null) defaultValue
        else try {
            enumValueOf(this)
        } catch (e: IllegalArgumentException) {
            executeIfDebugging {
                e.printStackTrace()
            }
            defaultValue
        }

}
