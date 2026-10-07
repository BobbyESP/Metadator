/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.component.ActionMenu
import com.bobbyesp.metadator.core.designsystem.component.Choice
import com.bobbyesp.metadator.core.designsystem.component.ConnectedChoices
import com.bobbyesp.metadator.core.designsystem.component.ItemIcon
import com.bobbyesp.metadator.core.designsystem.component.LoadingScreen
import com.bobbyesp.metadator.core.designsystem.component.MenuAction
import com.bobbyesp.metadator.core.designsystem.component.PlaceholderCard
import com.bobbyesp.metadator.core.designsystem.component.PopupMenu
import com.bobbyesp.metadator.core.designsystem.component.PopupMenuGroup
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackCollection
import com.bobbyesp.metadator.core.model.TrackSort
import com.bobbyesp.metadator.core.ui.component.ArtworkImage
import com.bobbyesp.metadator.core.ui.component.CollectionCard
import com.bobbyesp.metadator.core.ui.component.TrackListItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibraryScreen(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
    onOpenTrack: (Track) -> Unit,
    onOpenCollection: (TrackCollection) -> Unit,
    onEditTracks: (List<Track>) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permission = rememberAudioPermissionState()
    LaunchedEffect(permission.status) {
        if (permission.status == PermissionStatus.Granted) onIntent(LibraryIntent.Start)
    }
    BackHandler(enabled = state.selecting) { onIntent(LibraryIntent.ClearSelection) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedContent(targetState = state.selecting, label = "LibraryTopBar") { selecting ->
                if (selecting) {
                    SelectionTopBar(
                        count = state.selection.size,
                        onClose = { onIntent(LibraryIntent.ClearSelection) },
                        onSelectAll = { onIntent(LibraryIntent.SelectAll) },
                    )
                } else {
                    SearchTopBar(
                        query = state.filter.search,
                        onQueryChange = { onIntent(LibraryIntent.Search(it)) },
                        enabled = permission.status == PermissionStatus.Granted,
                        onOpenSettings = onOpenSettings,
                        onOpenFile = onOpenFile,
                        onRescan = { onIntent(LibraryIntent.Refresh) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                permission.status != PermissionStatus.Granted ->
                    PermissionNeeded(
                        permanentlyDenied = permission.status == PermissionStatus.PermanentlyDenied,
                        onGrant = permission::request,
                        onOpenFile = onOpenFile,
                    )
                state.loading -> LoadingScreen()
                else ->
                    LibraryContent(
                        state = state,
                        onIntent = onIntent,
                        onOpenTrack = onOpenTrack,
                        onOpenCollection = onOpenCollection,
                    )
            }

            AnimatedVisibility(
                visible = state.selecting,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        // Above the mini player when one is showing.
                        .padding(bottom = if (state.playerActive) 104.dp else Spacing.large),
            ) {
                SelectionToolbar(
                    onEdit = { onEditTracks(state.selectedTracks) },
                    onPlay = { onIntent(LibraryIntent.PlaySelection) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    enabled: Boolean,
    onOpenSettings: () -> Unit,
    onOpenFile: () -> Unit,
    onRescan: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = Spacing.screen,
                    end = Spacing.extraSmall,
                    top = Spacing.small,
                    bottom = Spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    stringResource(R.string.search_library),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            focusManager.clearFocus()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        )
        IconButton(onClick = onOpenSettings, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
        }
        Box {
            IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                )
            }
            ActionMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                actions =
                    listOfNotNull(
                        MenuAction(
                            stringResource(R.string.open_file),
                            Icons.Rounded.AudioFile,
                            onClick = onOpenFile,
                        ),
                        MenuAction(
                                stringResource(R.string.rescan_library),
                                Icons.Rounded.Refresh,
                                onClick = onRescan,
                            )
                            .takeIf { enabled },
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close_selection),
                )
            }
        },
        actions = {
            IconButton(onClick = onSelectAll, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    Icons.Rounded.SelectAll,
                    contentDescription = stringResource(R.string.select_all),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionToolbar(onEdit: () -> Unit, onPlay: () -> Unit) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit_together),
                )
            }
        },
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    ) {
        IconButton(onClick = onPlay, shapes = IconButtonDefaults.shapes()) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.play_selection),
            )
        }
    }
}

@Composable
private fun PermissionNeeded(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenFile: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(Spacing.extraLarge), contentAlignment = Alignment.Center) {
        PlaceholderCard(
            title = stringResource(R.string.permission_title),
            description =
                stringResource(
                    if (permanentlyDenied) R.string.permission_denied_description
                    else R.string.permission_description
                ),
            icon = Icons.Rounded.LibraryMusic,
            actionText =
                stringResource(
                    if (permanentlyDenied) R.string.permission_open_settings
                    else R.string.permission_grant
                ),
            onAction = onGrant,
            secondaryActionText = stringResource(R.string.open_file),
            onSecondaryAction = onOpenFile,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
    onOpenTrack: (Track) -> Unit,
    onOpenCollection: (TrackCollection) -> Unit,
) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Column(Modifier.fillMaxSize()) {
        ConnectedChoices(
            choices = LibraryTab.entries.map { Choice(it, stringResource(it.label), it.icon) },
            selected = state.tab,
            onSelect = { onIntent(LibraryIntent.SelectTab(it)) },
            modifier =
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen),
        )
        FilterRow(state, onIntent)

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { onIntent(LibraryIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.totalTracks == 0 ->
                    CenteredPlaceholder {
                        PlaceholderCard(
                            title = stringResource(R.string.empty_library_title),
                            description = stringResource(R.string.empty_library_description),
                            icon = Icons.Rounded.LibraryMusic,
                            actionText = stringResource(R.string.rescan_library),
                            actionIcon = Icons.Rounded.Refresh,
                            onAction = { onIntent(LibraryIntent.Refresh) },
                        )
                    }
                state.tracks.isEmpty() ->
                    CenteredPlaceholder {
                        PlaceholderCard(
                            title = stringResource(R.string.no_results_title),
                            description = stringResource(R.string.no_results_description),
                            icon = Icons.Rounded.SearchOff,
                            actionText = stringResource(R.string.clear_filters),
                            onAction = {
                                onIntent(LibraryIntent.Search(""))
                                onIntent(LibraryIntent.ClearFilters)
                            },
                        )
                    }
                else ->
                    AnimatedContent(
                        targetState = state.tab,
                        transitionSpec = {
                            // The new tab comes from the side its button is on.
                            val travel = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(spatial) { travel * it / 6 } + fadeIn(effects))
                                .togetherWith(
                                    slideOutHorizontally(spatial) { -travel * it / 6 } +
                                        fadeOut(effects)
                                )
                        },
                        label = "LibraryTab",
                    ) { tab ->
                        when (tab) {
                            LibraryTab.Songs -> SongList(state, onIntent, onOpenTrack)
                            LibraryTab.Albums -> AlbumGrid(state.albums, onOpenCollection)
                            LibraryTab.Artists ->
                                CollectionList(
                                    state.artists,
                                    Icons.Rounded.Person,
                                    onOpenCollection,
                                )
                            LibraryTab.Folders ->
                                CollectionList(
                                    state.folders,
                                    Icons.Rounded.FolderOpen,
                                    onOpenCollection,
                                )
                        }
                    }
            }
        }
    }
}

@Composable
private fun CenteredPlaceholder(content: @Composable () -> Unit) {
    // Scrollable, so pull-to-refresh still works on an empty library.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterRow(state: LibraryState, onIntent: (LibraryIntent) -> Unit) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            FilterChip(
                selected = false,
                onClick = { sortMenuOpen = true },
                label = { Text(stringResource(state.sort.sort.label)) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
            PopupMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                PopupMenuGroup(index = 0, count = 2) {
                    TrackSort.entries.forEachIndexed { index, sort ->
                        SelectableDropdownMenuItem(
                            selected = state.sort.sort == sort,
                            onClick = {
                                onIntent(LibraryIntent.Sort(sort))
                                sortMenuOpen = false
                            },
                            text = { Text(stringResource(sort.label)) },
                            shapes = MenuDefaults.itemShape(index, TrackSort.entries.size),
                            selectedLeadingIcon = { Icon(Icons.Rounded.Check, null) },
                        )
                    }
                }
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                PopupMenuGroup(index = 1, count = 2) {
                    DropdownMenuItem(
                        onClick = {
                            onIntent(LibraryIntent.ToggleSortDirection)
                            sortMenuOpen = false
                        },
                        text = {
                            Text(
                                stringResource(
                                    if (state.sort.ascending) R.string.sort_descending
                                    else R.string.sort_ascending
                                )
                            )
                        },
                        shape = MenuDefaults.itemShape(0, 1).shape,
                        leadingIcon = {
                            Icon(
                                if (state.sort.ascending) Icons.Rounded.ArrowDownward
                                else Icons.Rounded.ArrowUpward,
                                null,
                            )
                        },
                    )
                }
            }
        }
        if (state.needsAttentionCount > 0 || state.filter.needsAttention) {
            FilterChip(
                selected = state.filter.needsAttention,
                onClick = { onIntent(LibraryIntent.ToggleNeedsAttention) },
                label = {
                    Text(stringResource(R.string.needs_attention_filter, state.needsAttentionCount))
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Warning, null, Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
        if (state.formats.size > 1) {
            state.formats.forEach { format ->
                FilterChip(
                    selected = format in state.filter.formats,
                    onClick = { onIntent(LibraryIntent.ToggleFormat(format)) },
                    label = { Text(format.uppercase()) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SongList(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
    onOpenTrack: (Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = Spacing.floatingClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "header", contentType = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.song_count,
                        state.tracks.size,
                        state.tracks.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onIntent(LibraryIntent.PlayAll(shuffle = true)) },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(ButtonDefaults.IconSize))
                    Text(
                        stringResource(R.string.shuffle_all),
                        Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
                FilledIconButton(
                    onClick = { onIntent(LibraryIntent.PlayAll(shuffle = false)) },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.padding(start = Spacing.small),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_all),
                    )
                }
            }
        }
        itemsIndexed(
            state.tracks,
            key = { _, track -> track.id.value },
            contentType = { _, _ -> "track" },
        ) { index, track ->
            TrackListItem(
                track = track,
                selected = track.id in state.selection,
                selecting = state.selecting,
                isPlaying = track.id == state.playingId,
                isPlaybackRunning = state.playbackRunning,
                shapes = GroupShapes.listItemShapes(index, state.tracks.size),
                onClick = {
                    if (state.selecting) onIntent(LibraryIntent.ToggleSelection(track.id))
                    else onOpenTrack(track)
                },
                onLongClick = { onIntent(LibraryIntent.ToggleSelection(track.id)) },
                onPlay = { onIntent(LibraryIntent.Play(track)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<TrackCollection.Album>, onOpen: (TrackCollection) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = Spacing.floatingClearance,
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        items(albums, key = { it.key }) { album ->
            CollectionCard(
                title = album.title,
                subtitle =
                    listOfNotNull(album.artist, album.year?.toString())
                        .joinToString(" · ")
                        .ifEmpty { null },
                artwork = album.artworkRef?.uri,
                onClick = { onOpen(album) },
                modifier = Modifier.animateItem(),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Spacing.small)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionList(
    collections: List<TrackCollection>,
    icon: ImageVector,
    onOpen: (TrackCollection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                bottom = Spacing.floatingClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(collections, key = { _, it -> it.key }) { index, collection ->
            SegmentedListItem(
                onClick = { onOpen(collection) },
                modifier = Modifier.animateItem(),
                shapes = GroupShapes.listItemShapes(index, collections.size),
                verticalAlignment = Alignment.CenterVertically,
                colors =
                    ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                leadingContent = {
                    if (collection.artworkRef != null && collection !is TrackCollection.Folder) {
                        ArtworkImage(
                            model = collection.artworkRef?.uri,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            shape =
                                if (collection is TrackCollection.Artist) CircleShape
                                else MaterialTheme.shapes.medium,
                        )
                    } else {
                        ItemIcon(icon, Modifier.size(52.dp))
                    }
                },
                supportingContent = {
                    Text(
                        pluralStringResource(
                            R.plurals.song_count,
                            collection.tracks.size,
                            collection.tracks.size,
                        ),
                        maxLines = 1,
                    )
                },
            ) {
                Text(
                    collection.title,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val LibraryTab.label: Int
    get() =
        when (this) {
            LibraryTab.Songs -> R.string.tab_songs
            LibraryTab.Albums -> R.string.tab_albums
            LibraryTab.Artists -> R.string.tab_artists
            LibraryTab.Folders -> R.string.tab_folders
        }

private val LibraryTab.icon: ImageVector
    get() =
        when (this) {
            LibraryTab.Songs -> Icons.Rounded.MusicNote
            LibraryTab.Albums -> Icons.Rounded.Album
            LibraryTab.Artists -> Icons.Rounded.Person
            LibraryTab.Folders -> Icons.Rounded.FolderOpen
        }

private val TrackSort.label: Int
    get() =
        when (this) {
            TrackSort.Title -> R.string.sort_title
            TrackSort.Artist -> R.string.sort_artist
            TrackSort.Album -> R.string.sort_album
            TrackSort.DateAdded -> R.string.sort_date_added
            TrackSort.DateModified -> R.string.sort_date_modified
            TrackSort.Duration -> R.string.sort_duration
        }
