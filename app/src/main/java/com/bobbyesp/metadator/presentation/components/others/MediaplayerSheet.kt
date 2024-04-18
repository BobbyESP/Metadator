package com.bobbyesp.metadator.presentation.components.others

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
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
    DraggableBottomSheet(
        modifier = modifier,
        state = state,
        collapsedContent = {
            MediaplayerCollapsedContent(
                viewModel = viewModel,
            )
        },
        backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation)
    ) {
        MediaplayerExpandedContent(
            viewModel = viewModel,
            sheetState = state
        )
    }
}

@Composable
private fun MediaplayerCollapsedContent(
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val playingSong = viewState.playingSong ?: return

    val progress = (playerState as? MediaplayerViewModel.PlayerState.Ready)?.progress ?: 0f
    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CollapsedPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        SongCard(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            playingSong = playingSong,
            songProgress = progress,
            isPlaying = isPlaying,
        ) {
            viewModel.togglePlayPause()
        }
    }
}

@Composable
private fun MediaplayerExpandedContent(
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
    sheetState: DraggableBottomSheetState
) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val playingSong = viewState.queueSongs.items.getOrNull(0)?.mediaMetadata ?: return

    val progress = (playerState as? MediaplayerViewModel.PlayerState.Ready)?.progress ?: 0f
    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = sheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        SongInformation(
                            mediaMetadata = playingSong,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    ) {

                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = sheetState.collapsedBound)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                    }
                }
            }
        }
    }
}

@Composable
private fun SongInformation(
    modifier: Modifier = Modifier,
    mediaMetadata: MediaMetadata
) {
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArtworkAsyncImage(
            modifier = Modifier
                .size(screenWidth / 2)
                .clip(MaterialTheme.shapes.medium),
            artworkPath = mediaMetadata.artworkUri
        )
        MarqueeText(
            text = mediaMetadata.title.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        MarqueeText(
            text = mediaMetadata.artist.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            fontSize = 12.sp
        )
    }
}

@Composable
fun SongCard(
    modifier: Modifier = Modifier,
    playingSong: Song,
    isPlaying: Boolean = false,
    songProgress: Float = 0f,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ArtworkAsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                artworkPath = playingSong.artworkPath
            )
            Column(
                horizontalAlignment = Alignment.Start, modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .weight(1f)
            ) {
                MarqueeText(
                    text = playingSong.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                MarqueeText(
                    text = playingSong.artist,
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
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(
                            id = R.string.pause
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }, icon2 = {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            id = R.string.play
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }, isIcon1 = isPlaying
            ) {
                onClick()
            }
        }
        LinearProgressIndicator(
            progress = {
                songProgress
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SongInformationPrev() {
    MetadatorTheme {
        SongInformation(
            mediaMetadata = MediaMetadata.Builder()
                .setTitle("Bones")
                .setArtist("Imagine Dragons")
                .setAlbumTitle("Mercury - Acts 1 & 2")
                .setArtworkUri(null)
                .build()
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CollapsedContentPrev() {
    MetadatorTheme {
        SongCard(
            playingSong = Song(
                id = 1,
                title = "Bones",
                artist = "Imagine Dragons",
                album = "Mercury - Acts 1 & 2",
                artworkPath = null,
                duration = 100.0,
                path = "path"
            )
        )
    }
}