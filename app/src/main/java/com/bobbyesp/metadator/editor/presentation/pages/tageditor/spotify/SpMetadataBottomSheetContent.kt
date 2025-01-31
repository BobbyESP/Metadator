package com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify

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
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel.Companion.BottomSheetStage
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.stages.NoSongInformationProvided
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.stages.SpMetadataBsDetails
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.stages.SpMetadataBsSearch
import com.bobbyesp.ui.motion.MotionConstants.DURATION_EXIT_SHORT
import com.bobbyesp.ui.motion.tweenEnter
import com.bobbyesp.ui.motion.tweenExit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpMetadataBottomSheetContent(
    name: String,
    artist: String,
    sheetState: SheetState,
    bsViewState: State<MetadataBottomSheetViewModel.ViewState>,
    onEvent: (MetadataBottomSheetViewModel.Event) -> Unit,
    onCloseSheet: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    fun search(query: String) {
        onEvent(MetadataBottomSheetViewModel.Event.ChangeState(BottomSheetStage.SEARCH))
        onEvent(MetadataBottomSheetViewModel.Event.Search(query))
    }

    if (name.isEmpty() && artist.isEmpty()) {
        NoSongInformationProvided { providedName, providedArtist ->
            val query = "$providedName $providedArtist"
            search(query)
        }
    }

    LaunchedEffect(sheetState.isVisible, name, artist) {
        val query = "$name $artist"
        if (sheetState.isVisible && bsViewState.value.lastQuery != query) {
            search(query)
        }
    }

    AnimatedContent(
        targetState = bsViewState.value.stage,
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
                    listState = lazyListState,
                    pageViewState = bsViewState,
                    onChooseTrack = { track ->
                        onEvent(MetadataBottomSheetViewModel.Event.SelectTrack(track))
                    }
                )
            }

            BottomSheetStage.TRACK_DETAILS -> {
                SpMetadataBsDetails(
                    modifier = Modifier.fillMaxSize(),
                    onEvent = onEvent,
                    pageViewState = bsViewState,
                    onCloseSheet = onCloseSheet
                )
            }
        }
    }
}
