package com.bobbyesp.metadator.presentation.common

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

fun Any.qualifiedName(): String = this::class.qualifiedName.toString()