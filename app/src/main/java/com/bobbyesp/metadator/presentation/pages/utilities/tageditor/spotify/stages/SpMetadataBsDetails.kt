package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.bobbyesp.metadator.presentation.utils.SongCardBoundsTransformation
import com.bobbyesp.ui.motion.MotionConstants.DURATION_EXIT_SHORT
import com.bobbyesp.ui.motion.tweenEnter
import com.bobbyesp.ui.motion.tweenExit

context(SharedTransitionScope)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AnimatedVisibilityScope.SpMetadataBsDetails(
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
        LaunchedEffect(true) {
            selectedTrack = viewState.selectedTrack
        }
        selectedTrack?.let {
            SpotifyHorizontalSongCard(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "container_${it.id}"
                        ),
                        animatedVisibilityScope = this@SpMetadataBsDetails,
                        placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                        boundsTransform = SongCardBoundsTransformation,
                        enter = fadeIn(
                            tweenEnter(delayMillis = DURATION_EXIT_SHORT)
                        ),
                        exit = fadeOut(
                            tweenExit(durationMillis = DURATION_EXIT_SHORT)
                        )
                    ),
                imageModifier = Modifier.sharedElement(
                    state = rememberSharedContentState(
                        key = it.id
                    ),
                    animatedVisibilityScope = this@SpMetadataBsDetails,
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                    boundsTransform = SongCardBoundsTransformation,
                ),
                innerModifier = Modifier.padding(8.dp),
                surfaceColor = Color.Transparent,
                track = it,
                onClick = {

                }
            )
        }
    }
}