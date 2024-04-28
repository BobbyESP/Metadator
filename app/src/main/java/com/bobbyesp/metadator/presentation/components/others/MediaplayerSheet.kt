package com.bobbyesp.metadator.presentation.components.others

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.bobbyesp.mediaplayer.service.ConnectionState
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.buttons.PlayPauseAnimatedButton
import com.bobbyesp.metadator.presentation.components.image.ArtworkAsyncImage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheet
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import com.bobbyesp.ui.components.button.DynamicButton
import com.bobbyesp.ui.components.text.MarqueeText
import com.bobbyesp.ui.components.text.MarqueeTextGradientOptions
import com.bobbyesp.ui.motion.materialSharedAxisXIn
import com.bobbyesp.ui.motion.materialSharedAxisXOut
import com.bobbyesp.utilities.Time.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaplayerSheet(
    modifier: Modifier = Modifier,
    state: DraggableBottomSheetState,
    viewModel: MediaplayerViewModel
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

    DraggableBottomSheet(
        modifier = modifier, state = state, collapsedContent = {
            MediaplayerCollapsedContent(
                viewModel = viewModel, nowPlaying = playingSong
            )
        }, backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        MediaplayerExpandedContent(
            viewModel = viewModel,
            sheetState = state,
        )
    }
}

@Composable
private fun MediaplayerCollapsedContent(
    nowPlaying: MediaMetadata,
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val progress = (playerState as? MediaplayerViewModel.PlayerState.Ready)?.progress ?: 0f
    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CollapsedPlayerHeight)
            .padding(bottom = 8.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center
    ) {
        MiniplayerContent(
            modifier = Modifier.padding(horizontal = 12.dp),
            playingSong = nowPlaying,
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
    val scope = rememberCoroutineScope()

    BackHandler {
        sheetState.collapseSoft()
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                IconButton(onClick = {
                    scope.launch {
                        sheetState.collapseSoft()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = null,
                        modifier = Modifier.rotate(-90f)
                    )

                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
                }
            }
            PlayerControls(
                modifier = Modifier.fillMaxWidth(),
                imageModifier = Modifier,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControls(
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
) {
    val scope = rememberCoroutineScope()
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val playingSong =
        viewModel.playingSong.collectAsStateWithLifecycle().value?.mediaMetadata ?: return

    val readyState = playerState as? MediaplayerViewModel.PlayerState.Ready
    val progress = readyState?.progress ?: return

    var sliderPosition by remember {
        mutableStateOf<Float?>(null)
    }

    val duration by remember(readyState.duration) {
        mutableLongStateOf(readyState.duration)
    }

    var temporalProgressString by remember {
        mutableStateOf<String?>(null)
    }

    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArtworkAsyncImage(
            artworkPath = playingSong.artworkUri,
            modifier = imageModifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .clip(MaterialTheme.shapes.small)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = playingSong.title.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
            )
            MarqueeText(
                text = playingSong.artist.toString(),
                style = MaterialTheme.typography.bodyLarge,
                customEasing = EaseInOutSine,
                sideGradient = MarqueeTextGradientOptions(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    left = false
                )
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            val interactionSource = remember {
                MutableInteractionSource()
            }

            val songDuration by remember(readyState.duration) {
                derivedStateOf {
                    formatDuration(readyState.duration)
                }
            }

            val colors = SliderDefaults.colors()

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                modifier = Modifier.height(20.dp),
                value = sliderPosition ?: progress,
                onValueChange = {
                    sliderPosition = it
                    temporalProgressString = formatDuration((it * duration).toLong())
                },
                onValueChangeFinished = {
                    viewModel.seekTo(sliderPosition ?: return@Slider)
                    scope.launch {
                        delay(350)
                        sliderPosition = null
                        temporalProgressString = null
                    }
                },
                colors = colors,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 4.dp,
                        modifier = Modifier.height(8.dp)
                    )
                },
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(width = 4.dp, height = 20.dp)
                    )
                },
                interactionSource = interactionSource
            )

            Row(modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(
                    text = temporalProgressString ?: readyState.progressString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = songDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayPauseAnimatedButton(isPlaying = isPlaying) {
                    viewModel.togglePlayPause()
                }
            }
        }
    }
}

@Composable
fun MiniplayerContent(
    modifier: Modifier = Modifier,
    playingSong: MediaMetadata,
    isPlaying: Boolean = false,
    songProgress: Float = 0f,
    imageModifier: Modifier = Modifier,
    onPlayPause: () -> Unit = {}
) {
    val transitionState = remember { MutableTransitionState(playingSong) }

    LaunchedEffect(playingSong) {
        transitionState.targetState = playingSong
    }

    val transition = rememberTransition(transitionState = transitionState)

    val songCardArtworkUri = remember(transitionState.isIdle) {
        transitionState.currentState.artworkUri
    }

    val animatedSongProgress by animateFloatAsState(
        targetValue = songProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "Animated song progress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ArtworkAsyncImage(
                modifier = imageModifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                artworkPath = songCardArtworkUri
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .weight(1f)
            ) {
                transition.AnimatedContent(transitionSpec = {
                    ContentTransform(
                        materialSharedAxisXIn(initialOffsetX = { it / 10 }),
                        materialSharedAxisXOut(targetOffsetX = { -it / 10 }),
                        sizeTransform = SizeTransform(clip = true)
                    )
                }) {
                    Column {
                        MarqueeText(
                            text = it.title.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        MarqueeText(
                            text = it.artist.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            fontSize = 12.sp
                        )
                    }

                }
            }

            DynamicButton(
                modifier = Modifier
                    .size(42.dp)
                    .padding(4.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(
                            id = R.string.pause
                        ),
                    )
                }, icon2 = {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            id = R.string.play
                        ),
                    )
                }, isIcon1 = isPlaying
            ) {
                onPlayPause()
            }
        }
        LinearProgressIndicator(
            progress = {
                animatedSongProgress
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


//@Preview
//@Preview(uiMode = UI_MODE_NIGHT_YES)
//@Composable
//private fun SongInformationPrev() {
//    MetadatorTheme {
//        PlayerControls(
//            mediaMetadata = MediaMetadata.Builder().setTitle("Bones").setArtist("Imagine Dragons")
//                .setAlbumTitle("Mercury - Acts 1 & 2").setArtworkUri(null).build()
//        )
//    }
//}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CollapsedContentPrev() {
    MetadatorTheme {
        val metadata = MediaItem.Builder().setUri("path").setMediaMetadata(
            MediaMetadata.Builder().setTitle("Bones").setArtist("Imagine Dragons")
                .setAlbumTitle("Mercury - Acts 1 & 2").setArtworkUri(null).build()
        ).build()
        MiniplayerContent(
            playingSong = metadata.mediaMetadata
        )
    }
}