/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.model.CollectionType
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackCollection
import com.bobbyesp.metadator.core.navigation.BatchEditor
import com.bobbyesp.metadator.core.navigation.Collection
import com.bobbyesp.metadator.core.navigation.Editor
import com.bobbyesp.metadator.core.navigation.Library
import com.bobbyesp.metadator.core.navigation.Navigator
import com.bobbyesp.metadator.core.navigation.Settings
import com.bobbyesp.metadator.core.ui.viewmodel.ShowMessages
import com.bobbyesp.metadator.player.api.PlayerController
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** The library's destinations: the root list and the collections reached from it. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.librarySection(navigator: Navigator) {
    entry<Library>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { NoSongOpen() })
    ) {
        val viewModel: LibraryViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ShowMessages(viewModel.messages)
        val openFile = rememberOpenFileLauncher { uri -> navigator.goTo(Editor(uri.toString())) }

        LibraryScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onOpenTrack = { navigator.goTo(Editor(it.ref.uri)) },
            onOpenCollection = { collection ->
                navigator.goTo(Collection(collection.type, collection.key, collection.title))
            },
            onEditTracks = { tracks -> navigator.goTo(BatchEditor(tracks.map { it.ref.uri })) },
            onOpenSettings = { navigator.goTo(Settings) },
            onOpenFile = { openFile.launch(arrayOf("audio/*")) },
        )
    }

    entry<Collection>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { NoSongOpen() })
    ) { key ->
        val viewModel: LibraryViewModel = koinViewModel(key = "collection:${key.key}")
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.onIntent(LibraryIntent.Start) }
        val player: PlayerController = koinInject()

        val collections =
            when (key.type) {
                CollectionType.Album -> state.albums
                CollectionType.Artist -> state.artists
                CollectionType.Folder -> state.folders
            }
        val collection =
            remember(collections, key) { collections.firstOrNull { it.key == key.key } }

        CollectionScreen(
            title = key.title,
            collection = collection.takeUnless { state.loading },
            playingId = state.playingId,
            isPlaybackRunning = state.playbackRunning,
            showBack = true,
            onBack = navigator::goBack,
            onOpenTrack = { navigator.goTo(Editor(it.ref.uri)) },
            onPlay = { tracks, index, shuffle -> player.play(tracks, index, shuffle) },
            onEditAll = { tracks: List<Track> ->
                navigator.goTo(BatchEditor(tracks.map { it.ref.uri }))
            },
        )
    }
}

internal val TrackCollection.type: CollectionType
    get() =
        when (this) {
            is TrackCollection.Album -> CollectionType.Album
            is TrackCollection.Artist -> CollectionType.Artist
            is TrackCollection.Folder -> CollectionType.Folder
        }

/**
 * The system's file picker for one audio file. The grant is made persistable, so reopening the
 * editor after the app restarts can still read (and, if the provider allows, write) the file.
 */
@Composable
private fun rememberOpenFileLauncher(onPicked: (Uri) -> Unit) =
    LocalContext.current.let { context ->
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val resolver = context.contentResolver
            // Read and write when the provider grants both, read alone when it only grants that.
            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
                .onFailure {
                    runCatching {
                        resolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
            onPicked(uri)
        }
    }

/** What a wide window shows beside the library while no song is open. */
@Composable
private fun NoSongOpen() {
    Box(Modifier.fillMaxSize().padding(Spacing.extraLarge), contentAlignment = Alignment.Center) {
        PlaceholderCard(
            title = stringResource(R.string.no_song_open_title),
            description = stringResource(R.string.no_song_open_description),
            icon = Icons.Rounded.EditNote,
        )
    }
}
