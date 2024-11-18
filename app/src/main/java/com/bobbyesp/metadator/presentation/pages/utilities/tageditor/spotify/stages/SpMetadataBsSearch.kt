package com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.stages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.cards.songs.spotify.SpotifyHorizontalSongCard
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.MetadataBsVM
import com.bobbyesp.ui.components.button.ButtonWithIconAndText
import com.bobbyesp.ui.components.state.LoadingState
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.ui.pagingStateHandler

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpMetadataBsSearch(
    name: String,
    artist: String,
    listState: LazyListState = rememberLazyListState(),
    pageViewState: State<MetadataBsVM.ViewState>,
    onChooseTrack: (Track) -> Unit
) {
    val paginatedTracksState = pageViewState.value.searchedTracks
    val paginatedTracks = paginatedTracksState.data?.collectAsLazyPagingItems()

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
                                startY = 50f
                            )
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) { // TODO: Be able to change the query
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.showing_results_for).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    letterSpacing = 2.sp
                                )
                            )
                            Text(
                                text = name, style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.by))
                                    append(" ")
                                    withStyle(MaterialTheme.typography.titleMedium.toSpanStyle()) {
                                        append(artist)
                                    }
                                },
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ButtonWithIconAndText(
                                icon = Icons.Rounded.Edit,
                                text = stringResource(R.string.edit_query),
                                modifier = Modifier.weight(1f),
                                onClick = { /* TODO: Edit query */ },
                                enabled = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        when (paginatedTracksState) {
            is ResourceState.Loading -> {
                item {
                    LoadingState(stringResource(id = R.string.retrieving_spotify_token))
                }
            }

            is ResourceState.Success -> {
                items(
                    count = paginatedTracks!!.itemCount,
                    key = paginatedTracks.itemKey(),
                    contentType = paginatedTracks.itemContentType()
                ) { index ->
                    val item = paginatedTracks[index] ?: return@items
                    SpotifyHorizontalSongCard(
                        innerModifier = Modifier.padding(8.dp),
                        surfaceColor = Color.Transparent,
                        track = item,
                        onClick = {
                            onChooseTrack(item)
                        }
                    )
                }

                pagingStateHandler(paginatedTracks, itemCount = 1) {
                    LoadingState(stringResource(id = R.string.loading))
                }
            }

            is ResourceState.Error -> {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        contentColor = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier,
                                text = paginatedTracksState.message
                                    ?: stringResource(id = com.bobbyesp.ui.R.string.unknown_error_title),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}