package com.bobbyesp.metadator.presentation.components.cards.songs.compact

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.REDUCE_SHADOWS
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreferenceState
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize.Companion.toShape
import com.bobbyesp.metadator.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.presentation.components.text.ConditionedMarqueeText

@Composable
fun CompactSongCard(
    modifier: Modifier = Modifier,
    name: String,
    artists: String,
    artworkUri: Uri? = null,
    listIndex: Int? = null,
    shadow: Dp = 4.dp,
    size: CompactCardSize = CompactCardSize.LARGE,
    onClick: () -> Unit
) {
    val (reduceShadows, _) = rememberPreferenceState(REDUCE_SHADOWS)
    val cardSize by remember { mutableStateOf(size.value) }
    val formalizedShape = size.toShape()

    Box(
        modifier = modifier
            .shadow(if (reduceShadows.value) 0.dp else shadow, formalizedShape)
            .clip(formalizedShape)
            .size(cardSize)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            imageModel = artworkUri,
        )

        listIndex?.let {
            Text(
                text = "$it.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
            )
        }
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.scrim),
                        startY = 0f, endY = 500f
                    ),
                    alpha = if (reduceShadows.value) 0f else 0.6f
                )
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            ConditionedMarqueeText(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            if (artists.isNotEmpty()) {
                ConditionedMarqueeText(
                    text = artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}