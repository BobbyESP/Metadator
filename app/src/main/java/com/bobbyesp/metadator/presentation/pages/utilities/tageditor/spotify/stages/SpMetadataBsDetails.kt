package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.presentation.components.cards.songs.spotify.SpotifyHorizontalSongCard
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContentViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SpMetadataBsDetails(
    modifier: Modifier = Modifier,
    viewModel: SpMetadataBottomSheetContentViewModel
) {
    BackHandler {
        viewModel.clearTrack()
    }

    val viewState = viewModel.viewStateFlow.collectAsStateWithLifecycle().value

    var selectedTrack by rememberSaveable(key = "spSelectedTrack") {
        mutableStateOf<Track?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LaunchedEffect(viewState.selectedTrack) {
            selectedTrack = viewState.selectedTrack
        }
        selectedTrack?.let {
            SpotifyHorizontalSongCard(
                innerModifier = Modifier.padding(8.dp),
                surfaceColor = Color.Transparent,
                track = it,
                onClick = {

                }
            )
        }
    }
}