package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContentViewModel.Companion.BottomSheetStage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages.SpMetadataBsSearch
import com.bobbyesp.ui.motion.MotionConstants.DURATION_EXIT_SHORT
import com.bobbyesp.ui.motion.tweenEnter
import com.bobbyesp.ui.motion.tweenExit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpMetadataBottomSheetContent(
    name: String,
    artist: String,
    state: SheetState,
    viewModel: SpMetadataBottomSheetContentViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewStateFlow.collectAsStateWithLifecycle().value

    val bottomSheetState = viewState.stage

    if (name.isEmpty() && artist.isEmpty()) return

    LaunchedEffect(state.isVisible, name, artist) {
        if (state.isVisible) {
            viewModel.updateStage(BottomSheetStage.SEARCH)
            viewModel.searchTracks("$name $artist")
        }
    }

    AnimatedContent(
        targetState = bottomSheetState,
        label = "Transition between bs states",
        transitionSpec = {
            fadeIn(
                tweenEnter(delayMillis = DURATION_EXIT_SHORT)
            ) togetherWith fadeOut(
                tweenExit(durationMillis = DURATION_EXIT_SHORT)
            )
        }) { actualStage ->
        when (actualStage) {
            BottomSheetStage.SEARCH -> {
                SpMetadataBsSearch(
                    name = name,
                    artist = artist,
                    viewModel = viewModel
                )
            }

            BottomSheetStage.TRACK_DETAILS -> {

            }
        }
    }
}