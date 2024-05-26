package com.bobbyesp.metadator.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.ext.formatAsClassToRoute
import com.bobbyesp.metadator.ext.formatToRoute
import com.bobbyesp.metadator.model.ParcelableSong
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

@Serializable
object SettingsNavigator

@Serializable
object Settings

val routesToNavigate = listOf(
    MetadatorNavigator,
    MediaplayerNavigator
)

object NavigationUtilities {
    fun KClass<*>.getDestinationTitle(): Int? {
        val route = this.formatToRoute()
        return when (route) {
            MetadatorNavigator.formatAsClassToRoute() -> R.string.home
            MediaplayerNavigator.formatAsClassToRoute() -> R.string.mediaplayer
            MainHost.formatAsClassToRoute() -> R.string.app_name
            Home.formatAsClassToRoute() -> R.string.home
            Mediaplayer.formatAsClassToRoute() -> R.string.mediaplayer
            TagEditor.formatAsClassToRoute() -> R.string.tag_editor
            else -> null
        }
    }

    object IconsUtil {
        fun KClass<*>.getDestinationIcon(): ImageVector? {
            val route = this.formatToRoute()
            return when (route) {
                MetadatorNavigator.formatAsClassToRoute() -> Icons.Rounded.Home
                MediaplayerNavigator.formatAsClassToRoute() -> Icons.Rounded.PlayArrow
                MainHost.formatAsClassToRoute() -> Icons.Rounded.Home
                Home.formatAsClassToRoute() -> Icons.Rounded.Home
                Mediaplayer.formatAsClassToRoute() -> Icons.Rounded.PlayArrow
                TagEditor.formatAsClassToRoute() -> Icons.Rounded.Edit
                else -> null
            }
        }
    }
}