package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContentViewModel.Companion.BottomSheetStage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages.NoSongInformationProvided
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages.SpMetadataBsDetails
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
    val lazyListState = rememberLazyListState()

    val bottomSheetState = viewState.stage

    fun search(query: String) {
        viewModel.updateStage(BottomSheetStage.SEARCH)
        viewModel.searchTracks(query)
    }

    if (name.isEmpty() && artist.isEmpty()) {
        NoSongInformationProvided { providedName, providedArtist ->
            val query = "$providedName $providedArtist"
            search(query)
        }
    }

    LaunchedEffect(state.isVisible, name, artist) {
        val query = "$name $artist"
        if (state.isVisible && viewState.lastQuery != query) {
            search(query)
        }
    }

    AnimatedContent(targetState = bottomSheetState,
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
                    name = name, artist = artist, listState = lazyListState, viewModel = viewModel
                )
            }

            BottomSheetStage.TRACK_DETAILS -> {
                SpMetadataBsDetails(
                    modifier = Modifier.fillMaxSize(), viewModel = viewModel
                )
            }
        }
    }
}
