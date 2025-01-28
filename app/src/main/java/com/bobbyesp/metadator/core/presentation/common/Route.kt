package com.bobbyesp.metadator.core.presentation.common

import com.bobbyesp.metadator.domain.model.ParcelableSong
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object MetadatorNavigator : Route {
        @Serializable
        data object Home : Route {

            @Serializable
            data object VisualSettings : Route
        }
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
        data object Settings : Route {
            @Serializable
            data object General : Route

            @Serializable
            data object Appearance : Route

            @Serializable
            data object About : Route
        }
    }
}

val mainNavigators = listOf(
    Route.MetadatorNavigator,
    Route.MediaplayerNavigator
)

fun Any.qualifiedName(): String = this::class.qualifiedName.toString()