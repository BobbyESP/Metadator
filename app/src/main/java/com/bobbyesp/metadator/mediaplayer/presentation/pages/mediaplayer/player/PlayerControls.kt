package com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.buttons.PlayPauseAnimatedButton
import com.bobbyesp.metadator.presentation.components.others.RepeatStateIcon
import com.bobbyesp.metadator.presentation.components.others.ShuffleStateIcon
import com.bobbyesp.metadator.presentation.components.text.ConditionedMarqueeText
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.ui.components.text.MarqueeTextGradientOptions
import com.bobbyesp.utilities.Time
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
) {
    val scope = rememberCoroutineScope()

    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val playerState = viewState.uiState

    val readyState = playerState as? MediaplayerViewModel.PlayerState.Ready

    val progress = readyState?.progress ?: 0f

    val playingSong = viewModel.songBeingPlayed.collectAsStateWithLifecycle().value?.mediaMetadata

    var sliderPosition by remember {
        mutableStateOf<Float?>(null)
    }

    val duration by remember(readyState?.duration) {
        derivedStateOf {
            mutableLongStateOf(readyState?.duration ?: 0L)
        }
    }

    var temporalProgressString by remember {
        mutableStateOf<String?>(null)
    }

    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value
    val isShuffleEnabled = viewModel.isShuffleEnabled.collectAsStateWithLifecycle().value
    val repeatMode = viewModel.repeatMode.collectAsStateWithLifecycle().value

    val transitionState = remember { MutableTransitionState(playingSong) }

    LaunchedEffect(playingSong) {
        transitionState.targetState = playingSong
    }

    val transition = rememberTransition(transitionState = transitionState)


    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        transition.AnimatedContent(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.Start),
            transitionSpec = { AnimatedTextContentTransformation }) {
            Column {
                Text(
                    text = it?.title.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                )
                ConditionedMarqueeText(
                    text = it?.artist.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    customEasing = EaseInOutSine,
                    sideGradient = MarqueeTextGradientOptions(
                        color = MaterialTheme.colorScheme.surfaceContainer, left = false
                    )
                )
            }

        }
        Column(
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            val interactionSource = remember {
                MutableInteractionSource()
            }

            val songDuration by remember(readyState?.duration) {
                derivedStateOf {
                    Time.formatDuration(readyState?.duration ?: 0L)
                }
            }

            val colors = SliderDefaults.colors()

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                modifier = Modifier.height(20.dp),
                value = sliderPosition ?: progress,
                onValueChange = {
                    sliderPosition = it
                    temporalProgressString = Time.formatDuration((it * duration.longValue).toLong())
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
                    text = temporalProgressString ?: readyState?.progressString ?: "00:00",
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(PlayerCommandsButtonSize),
                    onClick = viewModel::toggleShuffle
                ) {
                    ShuffleStateIcon(
                        modifier = Modifier,
                        isShuffleEnabled = isShuffleEnabled
                    )
                }
                IconButton(
                    modifier = Modifier.size(SeekToButtonSize),
                    onClick = { viewModel.seekToPrevious() }) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(id = R.string.seek_to_previous)
                    )
                }
                PlayPauseAnimatedButton(isPlaying = isPlaying) {
                    viewModel.togglePlayPause()
                }
                IconButton(
                    modifier = Modifier.size(SeekToButtonSize),
                    onClick = { viewModel.seekToNext() }) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(id = R.string.seek_to_previous)
                    )
                }

                IconButton(
                    modifier = Modifier.size(PlayerCommandsButtonSize),
                    onClick = viewModel::toggleRepeat
                ) {
                    RepeatStateIcon(
                        modifier = Modifier,
                        repeatMode = repeatMode
                    )
                }
            }
        }
    }
}