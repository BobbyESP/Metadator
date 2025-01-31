package com.bobbyesp.metadator.mediastore.presentation.components.card.songs

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.core.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.core.presentation.components.text.ConditionedMarqueeText
import com.bobbyesp.metadator.core.presentation.theme.MetadatorTheme
import com.bobbyesp.utilities.mediastore.model.Song

@Composable
fun VerticalSongCard(
    modifier: Modifier = Modifier,
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small),
        onClick = onClick
    ) {
        Column {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                imageModel = song.artworkPath
            )
            Column(
                horizontalAlignment = Alignment.Start, modifier = Modifier.padding(8.dp)
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
        }
    }
}


@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun LocalSongCardPreview() {
    MetadatorTheme {
        VerticalSongCard(
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