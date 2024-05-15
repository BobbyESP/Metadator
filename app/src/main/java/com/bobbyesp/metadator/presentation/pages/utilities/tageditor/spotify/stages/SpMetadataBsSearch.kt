package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.cards.songs.spotify.SpotifyHorizontalSongCard
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContentViewModel
import com.bobbyesp.metadator.presentation.utils.pagination.pagingStateHandler

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpMetadataBsSearch(
    name: String,
    artist: String,
    listState: LazyListState = rememberLazyListState(),
    viewModel: SpMetadataBottomSheetContentViewModel,
) {
    val viewState = viewModel.viewStateFlow.collectAsStateWithLifecycle().value
    val paginatedTracks = viewState.searchedTracks.collectAsLazyPagingItems()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (name.isNotEmpty() && artist.isNotEmpty()) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow, Color.Transparent
                                ),
                                startY = 0f,
                            )
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.showing_results_for,
                            name,
                            artist
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                    )
                }
            }
        }

        items(
            count = paginatedTracks.itemCount,
            key = paginatedTracks.itemKey(),
            contentType = paginatedTracks.itemContentType()
        ) { index ->
            val item = paginatedTracks[index] ?: return@items
            SpotifyHorizontalSongCard(
                innerModifier = Modifier.padding(8.dp),
                surfaceColor = Color.Transparent,
                track = item,
                onClick = {
                    viewModel.chooseTrack(item)
                }
            )
        }
        pagingStateHandler(paginatedTracks, itemCount = 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}