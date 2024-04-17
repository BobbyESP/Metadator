package com.bobbyesp.metadator.presentation.components.others

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.image.ArtworkAsyncImage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import com.bobbyesp.model.Song
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheet
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import com.bobbyesp.ui.components.button.DynamicButton
import com.bobbyesp.ui.components.text.MarqueeText

@Composable
fun MediaplayerSheet(
    modifier: Modifier = Modifier,
    state: DraggableBottomSheetState,
    viewModel: MediaplayerViewModel
) {

    val songs = viewModel.pageViewState.value.playingSong
    DraggableBottomSheet(
        state = state,
        collapsedContent = {
            MediaplayerCollapsedContent(
                queueSongs = listOf(songs ?: return@DraggableBottomSheet)
            )
        },
        backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation)
    ) {

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaplayerCollapsedContent(
    modifier: Modifier = Modifier,
    queueSongs: List<Song>
) {
    val pagesCount = queueSongs.size

    val pagerState = rememberPagerState(pageCount = {
        pagesCount
    })

    HorizontalPager(modifier = modifier.fillMaxWidth(), state = pagerState) {
        val song = queueSongs.getOrNull(it) ?: return@HorizontalPager
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ArtworkAsyncImage(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp),
                    artworkPath = song.artworkPath
                )
                Column(
                    horizontalAlignment = Alignment.Start, modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                        .weight(1f)
                ) {
                    MarqueeText(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    MarqueeText(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        fontSize = 12.sp
                    )
                }

                DynamicButton(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.small
                        )
                        .padding(4.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                id = R.string.play
                            )
                        )
                    }, icon2 = {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = stringResource(
                                id = R.string.pause
                            )
                        )
                    }, isIcon1 = true
                ) {

                }
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CollapsedContentPrev() {
    MetadatorTheme {
        MediaplayerCollapsedContent(
            queueSongs = listOf(
                Song(
                    id = 1,
                    title = "Bones",
                    artist = "Imagine Dragons",
                    album = "Mercury - Acts 1 & 2",
                    artworkPath = null,
                    duration = 100.0,
                    path = "path"
                )
            )
        )
    }
}