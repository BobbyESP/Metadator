package com.bobbyesp.metadator.presentation.components.cards.songs.spotify

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.ext.formatArtistsName
import com.bobbyesp.metadator.presentation.components.image.AsyncImage
import com.bobbyesp.ui.components.text.MarqueeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotifyHorizontalSongCard(
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    track: Track,
    listIndex: Int? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val albumArtPath = track.album.images?.getOrNull(0)?.url

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = surfaceColor
    ) {
        Row(
            modifier = innerModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (listIndex != null) {
                Text(
                    text = "${listIndex + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(8.dp)
                        .padding(end = 4.dp)
                )
            }
            Box(contentAlignment = Alignment.CenterStart) {
                AsyncImage(
                    modifier = imageModifier.size(64.dp),
                    imageModel = albumArtPath,
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                ) {
                    MarqueeText(
                        text = track.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    MarqueeText(
                        text = track.artists.formatArtistsName(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}