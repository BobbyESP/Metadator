package com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.mediaplayer.service.ConnectionState
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer.views.MediaplayerCollapsedContent
import com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer.views.MediaplayerExpandedContent
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


