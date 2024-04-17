package com.bobbyesp.metadator.presentation.pages.mediaplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import com.bobbyesp.metadator.presentation.components.cards.songs.HorizontalSongCard
import com.bobbyesp.metadator.presentation.components.others.CollapsedPlayerHeight
import com.bobbyesp.metadator.presentation.components.others.MediaplayerSheet
import com.bobbyesp.metadator.presentation.components.others.PlayerAnimationSpec
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.components.pulltorefresh.rememberPullState
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionActionable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaplayerPage(
    viewModel: MediaplayerViewModel
) {
    val mediaStoreLazyColumnState = rememberLazyListState()
    val pullState = rememberPullState()

    val songs = viewModel.songsFlow.collectAsStateWithLifecycle(initialValue = emptyList()).value

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val mediaPlayerSheetState = rememberDraggableBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = CollapsedPlayerHeight,
            expandedBound = maxHeight,
            animationSpec = PlayerAnimationSpec,
        )

        Scaffold(
            modifier = Modifier.fillMaxSize()
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
                        state = mediaStoreLazyColumnState,
                    ) {
                        items(count = songs.size,
                            key = { index -> songs[index].id },
                            contentType = { index -> songs[index].id.toString() }) { index ->
                            val song = songs[index]
                            HorizontalSongCard(song = song,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = {
                                    viewModel.playSingleSong(song)
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