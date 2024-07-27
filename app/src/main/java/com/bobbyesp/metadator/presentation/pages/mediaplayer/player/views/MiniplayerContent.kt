package com.bobbyesp.metadator.presentation.pages.mediaplayer.player.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.player.AnimatedTextContentTransformation
import com.bobbyesp.ui.components.button.DynamicButton
import com.bobbyesp.ui.components.text.MarqueeText

@Composable
fun MiniplayerContent(
    modifier: Modifier = Modifier,
    playingSong: MediaMetadata,
    isPlaying: Boolean = false,
    songProgress: Float = 0f,
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
            AsyncImage(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                imageModel = songCardArtworkUri
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .weight(1f)
            ) {
                transition.AnimatedContent(transitionSpec = { AnimatedTextContentTransformation }) {
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

            DynamicButton(modifier = Modifier
                .size(42.dp)
                .padding(4.dp), icon = {
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