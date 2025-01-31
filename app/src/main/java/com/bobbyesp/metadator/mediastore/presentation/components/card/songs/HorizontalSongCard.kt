package com.bobbyesp.metadator.mediastore.presentation.components.card.songs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.core.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.core.presentation.components.text.ConditionedMarqueeText
import com.bobbyesp.metadator.core.presentation.theme.MetadatorTheme
import com.bobbyesp.utilities.Time
import com.bobbyesp.utilities.mediastore.model.Song

@Composable
fun HorizontalSongCard(
    modifier: Modifier = Modifier,
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp),
                imageModel = song.artworkPath
            )
            Column(
                horizontalAlignment = Alignment.Start, modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .weight(1f)
            ) {
                ConditionedMarqueeText(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                ConditionedMarqueeText(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    fontSize = 12.sp
                )
            }

            Text(
                text = Time.formatDuration(song.duration.toLong()),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.shapes.small
                    )
                    .padding(6.dp)
            )
        }
    }
}


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HorizontalSongCardPreview() {
    MetadatorTheme {
        HorizontalSongCard(
            modifier = Modifier.fillMaxWidth(),
            song = Song(
                id = 1,
                title = "Bones",
                artist = "Imagine Dragons",
                album = "Mercury - Acts 1 & 2",
                artworkPath = null,
                duration = 100.0,
                path = "path",
                fileName = "Bones"
            ), onClick = {})
    }
}