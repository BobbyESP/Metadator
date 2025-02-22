package com.bobbyesp.metadator.mediastore.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.mediastore.domain.enums.CompactCardSize
import com.bobbyesp.metadator.mediastore.domain.enums.LayoutType
import com.bobbyesp.metadator.mediastore.presentation.components.card.songs.HorizontalSongCard
import com.bobbyesp.metadator.mediastore.presentation.components.card.songs.compact.CompactSongCard
import com.bobbyesp.metadator.mediastore.presentation.components.others.status.EmptyMediaStoreWarning
import com.bobbyesp.ui.common.pages.ErrorPage
import com.bobbyesp.ui.common.pages.LoadingPage
import com.bobbyesp.utilities.mediastore.model.Song
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
    compactCardSize: CompactCardSize,
    onReloadMediaStore: () -> Unit,
    onItemClicked: (Song) -> Unit
) {
  val songsList = songs.value

  Crossfade(
      modifier = modifier.fillMaxSize(),
      targetState = desiredLayout,
      label = "List item transition",
      animationSpec = tween(200)) { type ->
        when (songsList) {
          is ResourceState.Loading ->
              LoadingPage(text = stringResource(R.string.loading_mediastore))

          is ResourceState.Error ->
              ErrorPage(
                  modifier = Modifier.fillMaxSize(),
                  throwable = Exception(songsList.message ?: stringResource(R.string.unknown))) {
                    onReloadMediaStore()
                  }

          is ResourceState.Success -> {
            val dataSongsList =
                songsList.data ?: throw IllegalStateException(stringResource(R.string.data_null))
            if (dataSongsList.isEmpty()) {
              EmptyMediaStoreWarning(modifier = Modifier.fillMaxSize())
            } else {
              when (type) {
                LayoutType.Grid -> {
                  LazyVerticalGridScrollbar(
                      state = lazyGridState,
                      settings =
                          ScrollbarSettings(
                              thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                              thumbSelectedColor = MaterialTheme.colorScheme.primary,
                              selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                          )) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(125.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                            state = lazyGridState) {
                              items(
                                  count = dataSongsList.size,
                                  key = { index -> dataSongsList[index].id },
                                  contentType = { _ -> "songItem" }) { index ->
                                    val song = dataSongsList[index]

                                    CompactSongCard(
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = null, fadeOutSpec = null),
                                        size = compactCardSize,
                                        name = song.title,
                                        artists = song.artist,
                                        artworkUri = song.artworkPath,
                                        onClick = { onItemClicked(song) })
                                  }
                            }
                      }
                }

                LayoutType.List -> {
                  LazyColumnScrollbar(
                      state = lazyListState,
                      settings =
                          ScrollbarSettings(
                              thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                              thumbSelectedColor = MaterialTheme.colorScheme.primary,
                              selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                          )) {
                        LazyColumn(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                            state = lazyListState,
                        ) {
                          items(
                              count = dataSongsList.size,
                              key = { index -> dataSongsList[index].id },
                              contentType = { _ -> "songHorizontalItem" }) { index ->
                                val song = dataSongsList[index]
                                HorizontalSongCard(
                                    song = song,
                                    modifier =
                                        Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                    onClick = { onItemClicked(song) })
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
