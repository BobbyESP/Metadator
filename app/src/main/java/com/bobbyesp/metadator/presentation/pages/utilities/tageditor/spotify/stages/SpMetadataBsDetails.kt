package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.ext.formatArtistsName
import com.bobbyesp.metadator.presentation.components.image.ArtworkAsyncImage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContentViewModel
import com.bobbyesp.ui.components.text.MarqueeText

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
        selectedTrack?.let { track ->
            TrackInfo(
                track = track
            )
        }
    }
}

@Composable
private fun TrackInfo(
    modifier: Modifier = Modifier,
    track: Track,
) {
    val albumArtPath = track.album.images?.getOrNull(0)?.url

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkAsyncImage(
            modifier = Modifier.size(64.dp),
            artworkPath = albumArtPath,
            shape = MaterialTheme.shapes.extraSmall
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                MarqueeText(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                MarqueeText(
                    text = track.artists.formatArtistsName(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}