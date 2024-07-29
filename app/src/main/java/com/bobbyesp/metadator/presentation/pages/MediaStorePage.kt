package com.bobbyesp.metadator.presentation.pages

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.presentation.components.cards.songs.HorizontalSongCard
import com.bobbyesp.metadator.presentation.components.cards.songs.VerticalSongCard
import com.bobbyesp.metadator.presentation.components.others.status.EmptyMediaStore
import com.bobbyesp.metadator.presentation.pages.home.LayoutType
import com.bobbyesp.utilities.model.Song
import com.bobbyesp.utilities.states.ResourceState
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionActionable
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun MediaStorePage(
    modifier: Modifier = Modifier,
    songs: State<ResourceState<List<Song>>>,
    lazyGridState: LazyGridState,
    lazyListState: LazyListState,
    desiredLayout: LayoutType,
    onItemClicked: (Song) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Crossfade(
            targetState = desiredLayout, label = "List item transition", animationSpec = tween(200)
        ) { type ->
            when (songs.value) {
                is ResourceState.Loading -> {
                    CircularProgressIndicator()
                }

                is ResourceState.Error -> {

                }

                is ResourceState.Success -> {
                    if (songs.value.data!!.isEmpty()) {
                        EmptyMediaStore(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val songsList = songs.value.data!!
                        when (type) {
                            LayoutType.Grid -> {
                                LazyVerticalGridScrollbar(
                                    state = lazyGridState, settings = ScrollbarSettings(
                                        thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        thumbSelectedColor = MaterialTheme.colorScheme.primary,
                                        selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                                    )
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
                                        items(count = songsList.size,
                                            key = { index -> songsList[index].id },
                                            contentType = { index -> songsList[index].id.toString() }) { index ->
                                            val song = songsList[index]
                                            VerticalSongCard(song = song,
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = null, fadeOutSpec = null
                                                ),
                                                onClick = {
                                                    onItemClicked(song)
                                                })
                                        }
                                    }
                                }
                            }

                            LayoutType.List -> {
                                LazyColumnScrollbar(
                                    state = lazyListState, settings = ScrollbarSettings(
                                        thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        thumbSelectedColor = MaterialTheme.colorScheme.primary,
                                        selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                                    )
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.background),
                                        state = lazyListState,
                                    ) {
                                        items(count = songsList.size,
                                            key = { index -> songsList[index].id },
                                            contentType = { index -> songsList[index].id.toString() }) { index ->
                                            val song = songsList[index]
                                            HorizontalSongCard(song = song,
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = null, fadeOutSpec = null
                                                ),
                                                onClick = {
                                                    onItemClicked(song)
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}