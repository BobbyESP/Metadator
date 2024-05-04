package com.bobbyesp.metadator.presentation.components.others.mediaplayer

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.bobbyesp.mediaplayer.service.ConnectionState
import com.bobbyesp.metadator.presentation.components.others.CollapsedPlayerHeight
import com.bobbyesp.metadator.presentation.components.others.mediaplayer.views.MediaplayerExpandedContent
import com.bobbyesp.metadator.presentation.components.others.mediaplayer.views.MiniplayerContent
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheet
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import kotlinx.coroutines.launch

@Composable
fun MediaplayerSheet(
    modifier: Modifier = Modifier, state: DraggableBottomSheetState, viewModel: MediaplayerViewModel
) {
    val playingSong =
        viewModel.playingSong.collectAsStateWithLifecycle().value?.mediaMetadata ?: return
    val connectionState =
        viewModel.connectionHandler.connectionState.collectAsStateWithLifecycle().value

    LaunchedEffect(connectionState, Unit) {
        if (connectionState is ConnectionState.Connected && state.isDismissed) {
            launch {
                state.collapseSoft()
            }
        }
    }

    DraggableBottomSheet(modifier = modifier, state = state, collapsedContent = {
        MediaplayerCollapsedContent(
            viewModel = viewModel, nowPlaying = playingSong
        )
    }, backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh, onDismiss = {
        viewModel.dismissPlayer()
    }) {
        MediaplayerExpandedContent(
            viewModel = viewModel,
            sheetState = state,
        )
    }
}

@Composable
private fun MediaplayerCollapsedContent(
    nowPlaying: MediaMetadata,
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val progress = (playerState as? MediaplayerViewModel.PlayerState.Ready)?.progress ?: 0f
    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CollapsedPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center
    ) {
        MiniplayerContent(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp),
            playingSong = nowPlaying,
            songProgress = progress,
            isPlaying = isPlaying,
        ) {
            viewModel.togglePlayPause()
        }
    }
}


@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CollapsedContentPrev() {
    MetadatorTheme {
        val metadata = MediaItem.Builder().setUri("path").setMediaMetadata(
            MediaMetadata.Builder().setTitle("Bones").setArtist("Imagine Dragons")
                .setAlbumTitle("Mercury - Acts 1 & 2").setArtworkUri(null).build()
        ).build()
        MiniplayerContent(
            playingSong = metadata.mediaMetadata
        )
    }
}