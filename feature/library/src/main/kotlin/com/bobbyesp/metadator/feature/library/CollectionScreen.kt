/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.LoadingScreen
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackCollection
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.core.ui.component.TrackListItem

/** The songs of one album, artist or folder, with what to do with all of them at once. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CollectionScreen(
    title: String,
    collection: TrackCollection?,
    playingTrack: Track?,
    showBack: Boolean,
    onBack: () -> Unit,
    onOpenTrack: (Track) -> Unit,
    onPlay: (tracks: List<Track>, index: Int, shuffle: Boolean) -> Unit,
    onEditAll: (List<Track>) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                subtitle =
                    collection?.let {
                        {
                            Text(
                                pluralStringResource(
                                    R.plurals.song_count,
                                    it.tracks.size,
                                    it.tracks.size,
                                )
                            )
                        }
                    },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                stringResource(R.string.back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (collection == null) {
            LoadingScreen(Modifier.padding(padding))
            return@Scaffold
        }
        val tracks = collection.tracks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    start = Spacing.screen,
                    end = Spacing.screen,
                    bottom = Spacing.floatingClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "header") {
                CollectionHeader(
                    collection = collection,
                    onPlay = { onPlay(tracks, 0, false) },
                    onShuffle = { onPlay(tracks, 0, true) },
                    onEditAll = { onEditAll(tracks) },
                )
            }
            itemsIndexed(tracks, key = { _, it -> it.id.value }) { index, track ->
                TrackListItem(
                    track = track,
                    onClick = { onOpenTrack(track) },
                    onLongClick = { onOpenTrack(track) },
                    onPlay = { onPlay(tracks, index, false) },
                    isPlaying = track.id == playingTrack?.id,
                    shapes = GroupShapes.listItemShapes(index, tracks.size),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionHeader(
    collection: TrackCollection,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onEditAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        if (collection !is TrackCollection.Folder) {
            ArtworkImage(
                model = collection.artworkRef?.uri,
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onPlay, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(ButtonDefaults.IconSize))
                Text(
                    stringResource(R.string.collection_play),
                    Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            FilledTonalButton(onClick = onEditAll, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Edit, null, Modifier.size(ButtonDefaults.IconSize))
                Text(
                    stringResource(R.string.collection_edit_all),
                    Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            OutlinedIconButton(onClick = onShuffle, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Shuffle, stringResource(R.string.collection_shuffle))
            }
        }
    }
}
