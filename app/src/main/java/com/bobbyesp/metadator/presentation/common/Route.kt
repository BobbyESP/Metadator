package com.bobbyesp.metadator.presentation.common

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object MainHost

@Serializable
object MetadatorNavigator

@Serializable
object Home

@Serializable
object MediaplayerNavigator

@Serializable
object Mediaplayer

@Serializable
object UtilitiesNavigator

@Serializable
data class TagEditor(
    val selectedSong: ParcelableSong
)

val routesToNavigate = listOf(
    MetadatorNavigator,
    MediaplayerNavigator
)

object NavigationUtilities {
    fun KClass<*>.getDestinationTitle(@ApplicationContext context: Context): String? {
        return when (this) {
            MainHost::class -> context.getString(R.string.app_name)
            Home::class -> context.getString(R.string.home)
            Mediaplayer::class -> context.getString(R.string.mediaplayer)
            TagEditor::class -> context.getString(R.string.tag_editor)
            else -> null
        }
    }

    fun Any.getDestinationTitle(): Int? {
        return when (this) {
            MainHost -> R.string.app_name
            Home -> R.string.home
            Mediaplayer -> R.string.mediaplayer
            TagEditor -> R.string.tag_editor
            else -> null
        }
    }

    object IconsUtil {
        fun KClass<*>.getDestinationIcon(): ImageVector? {
            return when (this) {
                MainHost::class -> Icons.Rounded.Home
                Home::class -> Icons.Rounded.Home
                Mediaplayer::class -> Icons.Rounded.PlayArrow
                TagEditor::class -> Icons.Rounded.Edit
                else -> null
            }
        }

        fun Any.getDestinationIcon(): ImageVector? {
            return when (this) {
                MainHost -> Icons.Rounded.Home
                Home -> Icons.Rounded.Home
                Mediaplayer -> Icons.Rounded.PlayArrow
                TagEditor -> Icons.Rounded.Edit
                else -> null
            }
        }
    }
}