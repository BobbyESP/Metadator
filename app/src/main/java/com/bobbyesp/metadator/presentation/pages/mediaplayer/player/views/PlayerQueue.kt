package com.bobbyesp.metadator.presentation.pages.mediaplayer.player.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import com.bobbyesp.metadator.presentation.components.image.AsyncImage

@Composable
fun PlayerQueue(
    modifier: Modifier = Modifier,
    imageModifier: Modifier,
    nowPlaying: MediaMetadata?,
    queue: List<MediaMetadata>,
    onPlay: (MediaMetadata) -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    BackHandler {
        onBackPressed()
    }

    Column {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            AsyncImage(
                imageModel = nowPlaying?.artworkUri,
                modifier = imageModifier
                    .clip(MaterialTheme.shapes.small)
            )
        }
    }
}