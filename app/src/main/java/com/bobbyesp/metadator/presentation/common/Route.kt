package com.bobbyesp.metadator.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object MainHost : Route

    @Serializable
    data object MetadatorNavigator : Route {
        @Serializable
        data object Home : Route
    }

    @Serializable
    data object MediaplayerNavigator : Route {
        @Serializable
        data object Mediaplayer : Route
    }

    @Serializable
    data object UtilitiesNavigator : Route {
        @Serializable
        data class TagEditor(val selectedSong: ParcelableSong) : Route
    }

    @Serializable
    data object SettingsNavigator : Route {
        @Serializable
        data object Settings : Route
    }
}

val routesToNavigate = listOf(
    Route.MetadatorNavigator,
    Route.MediaplayerNavigator
)

object NavigationUtilities {
    fun Route.getDestinationTitle(): Int? {
        return when (this) {
            is Route.MetadatorNavigator -> R.string.home
            is Route.MediaplayerNavigator -> R.string.mediaplayer
            is Route.MainHost -> R.string.app_name
            is Route.MetadatorNavigator.Home -> R.string.home
            is Route.MediaplayerNavigator.Mediaplayer -> R.string.mediaplayer
            is Route.UtilitiesNavigator.TagEditor -> R.string.tag_editor
            else -> null
        }
    }

    object IconsUtil {
        fun Route.getDestinationIcon(): ImageVector? {
            return when (this) {
                is Route.MetadatorNavigator -> Icons.Rounded.Home
                is Route.MediaplayerNavigator -> Icons.Rounded.PlayArrow
                is Route.MainHost -> Icons.Rounded.Home
                is Route.MetadatorNavigator.Home -> Icons.Rounded.Home
                is Route.MediaplayerNavigator.Mediaplayer -> Icons.Rounded.PlayArrow
                is Route.UtilitiesNavigator.TagEditor -> Icons.Rounded.Edit
                else -> null
            }
        }
    }
}