@file:OptIn(ExperimentalMaterial3Api::class)

package com.bobbyesp.metadator.presentation.pages.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.SONGS_LAYOUT
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.SONG_CARD_SIZE
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreference
import com.bobbyesp.metadator.core.ext.toParcelableSong
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.metadator.presentation.pages.MediaStorePage
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.ui.components.dropdown.AnimatedDropdownMenu
import com.bobbyesp.ui.components.dropdown.DropdownItemContainer
import com.bobbyesp.ui.components.text.AutoResizableText
import com.bobbyesp.utilities.mediastore.model.Song
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.ui.permission.PermissionNotGrantedDialog
import com.bobbyesp.utilities.ui.permission.PermissionRequestHandler
import com.bobbyesp.utilities.ui.permission.PermissionType.Companion.toPermissionType
import com.bobbyesp.utilities.ui.rememberForeverLazyGridState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    songs: State<ResourceState<List<Song>>>,
    onEvent: (MediaStorePageViewModel.Companion.Events) -> Unit = {}
) {
    val context = LocalActivity.current
    val readAudioFiles = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_EXTERNAL_STORAGE

        else -> Manifest.permission.READ_MEDIA_AUDIO
    }

    val storagePermissionState = rememberPermissionState(permission = readAudioFiles)

    LaunchedEffect(storagePermissionState.status.isGranted) {
        if (storagePermissionState.status.isGranted && songs.value !is ResourceState.Success) {
            onEvent(MediaStorePageViewModel.Companion.Events.StartObservingMediaStore)
        }
    }

    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    var moreOptionsVisible by remember {
        mutableStateOf(false)
    }

    val mediaStoreLazyGridState = rememberForeverLazyGridState(key = "lazyGrid")
    val mediaStoreLazyColumnState = rememberLazyListState()

    var configDesiredLayout = rememberPreference(SONGS_LAYOUT)

    var configDesiredCardSize = rememberPreference(SONG_CARD_SIZE)

    val gridIsFirstItemVisible by remember {
        derivedStateOf {
            mediaStoreLazyGridState.firstVisibleItemIndex == 0
        }
    }
    val listIsFirstItemVisible by remember {
        derivedStateOf {
            mediaStoreLazyColumnState.firstVisibleItemIndex == 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 4.sp,
                            ),
                        )
                        AutoResizableText(
                            text = stringResource(id = R.string.app_desc).uppercase(),
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall.copy(
                                letterSpacing = 2.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }, actions = {
                    IconButton(
                        onClick = { navController.navigate(Route.SettingsNavigator.Settings) }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(
                                id = R.string.settings
                            )
                        )
                    }
                    IconButton(
                        onClick = {
                            moreOptionsVisible = !moreOptionsVisible
                        }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(
                                id = R.string.open_more_options
                            )
                        )
                    }
                    AnimatedDropdownMenu(
                        expanded = moreOptionsVisible, onDismissRequest = {
                            moreOptionsVisible = false
                        }) {
                        DropdownMenuContent(
                            desiredLayout = LayoutType.valueOf(configDesiredLayout.value),
                            onLayoutChanged = {
                                configDesiredLayout.value = it.name
                            }, navigateToDialog = {
                                navController.navigate(Route.MetadatorNavigator.Home.VisualSettings)
                            },
                            navigateToMediaplayer = {
                                scope.launch {
                                    moreOptionsVisible = false
                                }
                                navController.navigate(Route.MediaplayerNavigator)
                            }
                        )
                    }

                })
        }, floatingActionButton = {
            when (LayoutType.valueOf(configDesiredLayout.value)) {
                LayoutType.Grid -> {
                    AnimatedVisibility(
                        visible = !gridIsFirstItemVisible,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    mediaStoreLazyGridState.animateScrollToItem(0)
                                }
                            }) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardDoubleArrowUp,
                                contentDescription = stringResource(
                                    id = R.string.scroll_to_top
                                )
                            )
                        }
                    }
                }

                LayoutType.List -> {
                    AnimatedVisibility(
                        visible = !listIsFirstItemVisible,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FloatingActionButton(onClick = {
                            scope.launch {
                                mediaStoreLazyColumnState.animateScrollToItem(0)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardDoubleArrowUp,
                                contentDescription = stringResource(
                                    id = R.string.scroll_to_top
                                )
                            )
                        }
                    }
                }
            }
        }) { paddingValues ->
        PermissionRequestHandler(
            permissionState = storagePermissionState,
            deniedContent = { shouldShowRationale ->
                PermissionNotGrantedDialog(
                    neededPermissions = persistentListOf(readAudioFiles.toPermissionType()),
                    onGrantRequest = {
                        storagePermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = {
                        context?.finish()
                    },
                    shouldShowRationale = shouldShowRationale
                )
            },
            content = {
                MediaStorePage(
                    modifier = Modifier.padding(paddingValues = paddingValues),
                    songs = songs,
                    lazyGridState = mediaStoreLazyGridState,
                    lazyListState = mediaStoreLazyColumnState,
                    desiredLayout = LayoutType.valueOf(configDesiredLayout.value),
                    compactCardSize = CompactCardSize.valueOf(configDesiredCardSize.value),
                    onReloadMediaStore = {
                        onEvent(MediaStorePageViewModel.Companion.Events.ReloadMediaStore)
                    },
                    onItemClicked = { song ->
                        navController.navigate(
                            Route.UtilitiesNavigator.TagEditor(song.toParcelableSong())
                        )
                    })
            })
    }
}

@Composable
private fun DropdownMenuContent(
    desiredLayout: LayoutType,
    onLayoutChanged: (LayoutType) -> Unit,
    navigateToDialog: () -> Unit = {},
    navigateToMediaplayer: () -> Unit = {}
) {
    val availableLayoutType = LayoutType.entries.toImmutableList()

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.layout_type),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
        DropdownItemContainer(
            modifier = Modifier,
            content = {
                SingleChoiceSegmentedButtonRow {
                    availableLayoutType.forEachIndexed { index, listType ->
                        SegmentedButton(
                            selected = desiredLayout.ordinal == listType.ordinal,
                            onClick = {
                                onLayoutChanged(listType)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index, count = availableLayoutType.size
                            ),
                        ) {
                            Icon(
                                imageVector = listType.icon,
                                contentDescription = stringResource(id = R.string.list_type)
                            )
                        }
                    }
                }
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = null
                )
            },
            text = { Text(stringResource(id = R.string.open_more_options)) },
            onClick = { navigateToDialog() }
        )

        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
            },
            text = { Text(stringResource(id = R.string.mediaplayer)) },
            onClick = { navigateToMediaplayer() }
        )
    }
}

