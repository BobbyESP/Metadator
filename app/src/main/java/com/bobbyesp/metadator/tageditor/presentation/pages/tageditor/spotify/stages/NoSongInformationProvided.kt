package com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.spotify.stages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.tageditor.presentation.components.textfield.PreConfiguredOutlinedTextField

@Composable
fun NoSongInformationProvided(onRetrySearch: (String, String) -> Unit) {
    val (name, setName) = remember { mutableStateOf("") }
    val (artist, setArtist) = remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Rounded.MoodBad,
            contentDescription = stringResource(id = R.string.no_song_information_provided),
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.no_song_information_provided),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                PreConfiguredOutlinedTextField(
                    value = name,
                    label = stringResource(id = R.string.title),
                    modifier = Modifier.weight(0.5f),
                ) { title ->
                    setName(title)
                }
                Spacer(modifier = Modifier.width(8.dp))
                PreConfiguredOutlinedTextField(
                    value = artist,
                    label = stringResource(id = R.string.artist),
                    modifier = Modifier.weight(0.5f),
                ) { artist ->
                    setArtist(artist)
                }
            }
        }
        Button(modifier = Modifier.fillMaxWidth(0.7f), onClick = { onRetrySearch(name, artist) }) {
            Text(
                text = stringResource(id = R.string.retry_search),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
