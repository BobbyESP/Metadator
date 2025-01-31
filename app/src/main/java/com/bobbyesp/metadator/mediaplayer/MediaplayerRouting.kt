package com.bobbyesp.metadator.mediaplayer

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerPage
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import com.bobbyesp.ui.motion.animatedComposable

fun NavGraphBuilder.mediaplayerRouting(
    mediaplayerViewModel: MediaplayerViewModel,
    mediaPlayerSheetState: DraggableBottomSheetState,
    onNavigateBack: () -> Unit
) {
    navigation<Route.MediaplayerNavigator>(
        startDestination = Route.MediaplayerNavigator.Mediaplayer,
    ) {
        animatedComposable<Route.MediaplayerNavigator.Mediaplayer> {
            MediaplayerPage(mediaplayerViewModel, mediaPlayerSheetState)
        }
    }
}