package com.bobbyesp.metadator.mediaplayer

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.ui.motion.animatedComposable

fun NavGraphBuilder.mediaplayerRouting(
    //mediaplayerViewModel: MediaplayerViewModel,
    onNavigateBack: () -> Unit
) {
    navigation<Route.MediaplayerNavigator>(
        startDestination = Route.MediaplayerNavigator.Mediaplayer,
    ) {
        animatedComposable<Route.MediaplayerNavigator.Mediaplayer> {

        }
    }
}