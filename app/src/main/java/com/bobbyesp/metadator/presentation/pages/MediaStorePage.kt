package com.bobbyesp.metadator.presentation.pages

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.cards.songs.LocalSongCard
import com.bobbyesp.model.Song
import com.bobbyesp.ui.components.pulltorefresh.PullToRefreshLayout
import com.bobbyesp.ui.components.pulltorefresh.rememberPullState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyGridVerticalScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionActionable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaStorePage(
    modifier: Modifier = Modifier,
    viewModel: MediaStorePageViewModel,
    lazyGridState: LazyGridState,
    onItemClicked: (Song) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle()
    val songs = viewState.value.songs
    val state = viewState.value.state

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Crossfade(
            targetState = state,
            label = "Fade between UI states MediaStorePage",
            modifier = Modifier.fillMaxSize()
        ) {
            when (it) {
                is MediaStorePageState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp, alignment = Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.loading_mediastore),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }

                is MediaStorePageState.Loaded -> {
                    if (songs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                8.dp, alignment = Alignment.CenterVertically
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_songs_found),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.9f))
                            Text(
                                text = stringResource(id = R.string.first_open_media_store),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    viewModel.loadMediaStoreTracks(
                                        context
                                    )
                                }
                            }) {
                                Text(text = stringResource(id = R.string.refresh))
                            }
                        }
                    } else {
                        val pullState = rememberPullState()

                        LaunchedEffect(pullState.isRefreshing) {
                            if (pullState.isRefreshing) {
                                viewModel.silentMediaStoreTracksLoad(context) {
                                    pullState.finishRefresh(skipReloadFinished = false)
                                }
                            }
                        }
                        PullToRefreshLayout(
                            pullState = pullState,
                        ) {
                            LazyGridVerticalScrollbar(
                                state = lazyGridState,
                                thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                thumbSelectedColor = MaterialTheme.colorScheme.primary,
                                selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(125.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    state = lazyGridState
                                ) {
                                    items(count = songs.size,
                                        key = { index -> songs[index].id },
                                        contentType = { index -> songs[index].id.toString() }) { index ->
                                        val song = songs[index]
                                        LocalSongCard(song = song,
                                            modifier = Modifier.animateItemPlacement(),
                                            onClick = {
                                                onItemClicked(song)
                                            })
                                    }
                                }
                            }
                        }
                    }
                }

                is MediaStorePageState.Error -> {
                    Text(text = "Error") //TODO: Change this
                }
            }
        }
    }
}