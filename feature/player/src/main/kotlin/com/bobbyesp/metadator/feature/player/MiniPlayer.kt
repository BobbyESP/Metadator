/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.dissolved
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.player.api.PlaybackState
import com.bobbyesp.metadator.player.api.PlayerController

/**
 * The player as a bar: what is playing, play and next. It is the collapsed content of
 * [PlayerSheet], which owns how it appears, opens and is dragged; the modifiers are how the sheet
 * ties the pieces it shares with the full player.
 *
 * @param shadowElevation the bar's shadow; none where the sheet lifts it with a blur halo
 * @param modifier applied to the bar's surface
 * @param contentModifier applied to what is on the surface, as a whole
 * @param artworkShape the cover's shape, which the sheet changes on the way to the full player
 * @param ownContentPresence how much of what only the bar has (next, the progress) is there, from 0
 *   to 1, read while drawing so that a transition does not recompose the bar
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MiniPlayerBar(
    track: Track,
    playback: PlaybackState,
    player: PlayerController,
    onOpen: () -> Unit,
    shape: Shape,
    artworkShape: Shape,
    shadowElevation: Dp,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    ownContentPresence: () -> Float = { 1f },
) {
    val openLabel = stringResource(R.string.open_player)
    Surface(
        onClick = onOpen,
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = shadowElevation,
        modifier =
            Modifier.fillMaxWidth().then(modifier).semantics {
                onClick(label = openLabel) {
                    onOpen()
                    true
                }
            },
    ) {
        Column(contentModifier) {
            // The cover's corners are the bar's, less this padding: the two curves stay parallel.
            Row(
                modifier = Modifier.padding(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(track, artworkShape, Modifier.size(48.dp).then(artworkModifier))
                // Each line as wide as its text and no wider: those are the bounds it travels from.
                // Set as the full player sets them, only smaller, so that each grows into its own.
                Column(Modifier.weight(1f).padding(horizontal = Spacing.large)) {
                    TrackLine(track, titleModifier) { shown ->
                        Text(
                            shown.title,
                            style = TrackNameDefaults.barTitleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TrackLine(track, artistModifier, follows = true) { shown ->
                        Text(
                            shown.artist ?: stringResource(R.string.unknown_artist),
                            style = TrackNameDefaults.barArtistStyle,
                            color = TrackNameDefaults.artistColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                PlayPauseButton(
                    isPlaying = playback.isPlaying,
                    onToggle = player::togglePlayPause,
                    // The sheet's modifier first: it is what sizes the button on the way out.
                    modifier = playButtonModifier.size(PlayPauseButtonDefaults.CompactSize),
                )
                IconButton(
                    onClick = player::skipToNext,
                    enabled = playback.hasNext,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.dissolved(ownContentPresence),
                ) {
                    Icon(Icons.Rounded.SkipNext, stringResource(R.string.next))
                }
            }
            val isPlaying = playback.isPlaying
            LinearWavyProgressIndicator(
                progress = { playback.progress },
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Spacing.extraLarge)
                        .padding(bottom = Spacing.medium)
                        .dissolved(ownContentPresence),
                amplitude = { if (isPlaying) 1f else 0f },
            )
        }
    }
}

/**
 * The key the cover is cached under. The bar and the full player ask for it at different sizes;
 * sharing the key lets the one arriving start from the picture the other already has.
 */
internal val Track.artworkCacheKey: String?
    get() = artworkRef?.uri?.let { "artwork:$it" }

/**
 * Android 13 needs permission to show the media notification. It is asked the first time something
 * plays, when the user can see why, and never again if refused.
 */
@Composable
internal fun RequestNotificationsOnFirstPlay(active: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(active) {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (active && !asked && !granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
