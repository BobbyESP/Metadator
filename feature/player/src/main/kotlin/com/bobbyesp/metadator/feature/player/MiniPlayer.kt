/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.player.api.PlayerController
import org.koin.compose.koinInject

/**
 * The player, collapsed: a floating card over the library while something plays. Tapping it opens
 * the full player.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(visible: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val player: PlayerController = koinInject()
    val playback by player.state.collectAsStateWithLifecycle()
    val track = playback.current
    RequestNotificationsOnFirstPlay(active = playback.isActive)

    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible && track != null,
        // A spring, so the card lands with a little bounce instead of sliding to a stop.
        enter =
            slideInVertically(motion.defaultSpatialSpec()) { it * 2 } +
                fadeIn(motion.defaultEffectsSpec()),
        exit =
            slideOutVertically(motion.fastSpatialSpec()) { it * 2 } +
                fadeOut(motion.fastEffectsSpec()),
        modifier = modifier,
    ) {
        if (track == null) return@AnimatedVisibility
        val openLabel = stringResource(R.string.open_player)
        Surface(
            onClick = onOpen,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 6.dp,
            modifier =
                Modifier.widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .semantics {
                        onClick(label = openLabel) {
                            onOpen()
                            true
                        }
                    },
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(
                        model = track.artworkRef?.uri,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.large,
                    )
                    // A new song pushes the old one's name up and out.
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            (slideInVertically(motion.defaultSpatialSpec()) { it } +
                                fadeIn(motion.defaultEffectsSpec())) togetherWith
                                (slideOutVertically(motion.fastSpatialSpec()) { -it } +
                                    fadeOut(motion.fastEffectsSpec()))
                        },
                        contentKey = { it.id },
                        modifier = Modifier.weight(1f).padding(horizontal = Spacing.medium),
                        label = "MiniPlayerTrack",
                    ) { shown ->
                        Column {
                            Text(
                                shown.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                shown.artist ?: stringResource(R.string.unknown_artist),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    PlayPauseButton(
                        isPlaying = playback.isPlaying,
                        onToggle = player::togglePlayPause,
                        iconSize = IconButtonDefaults.smallIconSize,
                    )
                    IconButton(
                        onClick = player::skipToNext,
                        enabled = playback.hasNext,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.next))
                    }
                }
                val isPlaying = playback.isPlaying
                LinearWavyProgressIndicator(
                    progress = { playback.progress },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = Spacing.large)
                            .padding(bottom = Spacing.small),
                    amplitude = { if (isPlaying) 1f else 0f },
                )
            }
        }
    }
}

/**
 * Android 13 needs permission to show the media notification. It is asked the first time something
 * plays, when the user can see why, and never again if refused.
 */
@Composable
private fun RequestNotificationsOnFirstPlay(active: Boolean) {
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
