package com.bobbyesp.metadator.presentation.pages.mediaplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.presentation.common.LocalPlayerAwareWindowInsets
import com.bobbyesp.metadator.presentation.components.cards.songs.HorizontalSongCard
import com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer.MediaplayerSheet
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionActionable

@Composable
fun MediaplayerPage(
    viewModel: MediaplayerViewModel,
    mediaPlayerSheetState: DraggableBottomSheetState
) {
    val mediaStoreLazyColumnState = rememberLazyListState()

    val songs = viewModel.songsFlow.collectAsStateWithLifecycle(initialValue = emptyList()).value

    val insets = LocalPlayerAwareWindowInsets.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = insets,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                LazyColumnScrollbar(
                    listState = mediaStoreLazyColumnState,
                    thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    thumbSelectedColor = MaterialTheme.colorScheme.primary,
                    selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        state = mediaStoreLazyColumnState,
                    ) {
                        items(count = songs.size,
                            key = { index -> songs[index].id },
                            contentType = { index -> songs[index].id.toString() }) { index ->
                            val song = songs[index]
                            HorizontalSongCard(song = song,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                ),
                                onClick = {
                                    viewModel.playShuffledQueue(song)

                                    if (mediaPlayerSheetState.isDismissed) {
                                        mediaPlayerSheetState.collapseSoft()
                                    }
                                })
                        }
                    }
                }
            }
        }

        MediaplayerSheet(
            state = mediaPlayerSheetState,
            viewModel = viewModel
        )
    }
}