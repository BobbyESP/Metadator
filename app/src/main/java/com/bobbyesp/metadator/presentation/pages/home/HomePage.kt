@file:OptIn(ExperimentalMaterial3Api::class)

package com.bobbyesp.metadator.presentation.pages.home

import android.Manifest
import android.app.Activity
import android.os.Build
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
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.ext.toParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalDrawerState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.TagEditor
import com.bobbyesp.metadator.presentation.pages.MediaStorePage
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.ui.components.dropdown.AnimatedDropdownMenu
import com.bobbyesp.ui.components.dropdown.DropdownItemContainer
import com.bobbyesp.ui.components.text.AutoResizableText
import com.bobbyesp.utilities.preferences.Preferences
import com.bobbyesp.utilities.preferences.PreferencesKeys.DESIRED_OVERLAY
import com.bobbyesp.utilities.ui.permission.PermissionNotGrantedDialog
import com.bobbyesp.utilities.ui.permission.PermissionRequestHandler
import com.bobbyesp.utilities.ui.permission.toPermissionType
import com.bobbyesp.utilities.ui.rememberForeverLazyGridState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier, viewModel: MediaStorePageViewModel
) {
    val context = LocalContext.current as Activity
    val currentApiVersion = Build.VERSION.SDK_INT
    val readAudioFiles = when {
        currentApiVersion < Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_EXTERNAL_STORAGE

        else -> Manifest.permission.READ_MEDIA_AUDIO
    }

    val storagePermissionState = rememberPermissionState(permission = readAudioFiles)

    val navController = LocalNavController.current
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    var moreOptionsVisible by remember {
        mutableStateOf(false)
    }

    val mediaStoreLazyGridState = rememberForeverLazyGridState(key = "lazyGrid")
    val mediaStoreLazyColumnState = rememberLazyListState()

    var desiredLayout by remember {
        mutableStateOf(
            Preferences.Enumerations.getValue(
                DESIRED_OVERLAY, LayoutType.Grid
            )
        )
    }

    val gridIsFirstItemVisible by remember { derivedStateOf { mediaStoreLazyGridState.firstVisibleItemIndex == 0 } }
    val listIsFirstItemVisible by remember { derivedStateOf { mediaStoreLazyColumnState.firstVisibleItemIndex == 0 } }

    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = stringResource(id = R.string.open_navigation)
                    )
                }
            }, title = {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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
                    onClick = {
                        moreOptionsVisible = !moreOptionsVisible
                    }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(
                            id = R.string.open_more_options
                        )
                    )
                }
                AnimatedDropdownMenu(
                    expanded = moreOptionsVisible, onDismissRequest = {
                        moreOptionsVisible = false
                    }) {
                    DropdownMenuContent(onLayoutChanged = {
                        desiredLayout = it
                    })
                }

            })
    }, floatingActionButton = {
        when (desiredLayout) {
            LayoutType.Grid -> {
                AnimatedVisibility(
                    visible = !gridIsFirstItemVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(onClick = {
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
        //for scrolling up to the top

    }) { paddingValues ->
        PermissionRequestHandler(permissionState = storagePermissionState,
            deniedContent = { shouldShowRationale ->
                PermissionNotGrantedDialog(
                    neededPermissions = listOf(readAudioFiles.toPermissionType()),
                    onGrantRequest = {
                        storagePermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = {
                        context.finish()
                    },
                    shouldShowRationale = shouldShowRationale
                )
            },
            content = {
                MediaStorePage(modifier = Modifier.padding(paddingValues = paddingValues),
                    viewModel = viewModel,
                    lazyGridState = mediaStoreLazyGridState,
                    lazyListState = mediaStoreLazyColumnState,
                    desiredLayout = desiredLayout,
                    onItemClicked = { song ->
                        navController.navigate(
                            TagEditor(song.toParcelableSong())
                        )
                    })
            })
    }
}

@Composable
private fun DropdownMenuContent(
    onLayoutChanged: (LayoutType) -> Unit = {}
) {
    val availableLayoutType = LayoutType.entries.toImmutableList()

    var desiredOverlay by remember {
        mutableIntStateOf(
            Preferences.Enumerations.getValue(
                DESIRED_OVERLAY, LayoutType.Grid
            ).ordinal
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.layout_type),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
        DropdownItemContainer(content = {
            SingleChoiceSegmentedButtonRow {
                availableLayoutType.forEachIndexed { index, listType ->
                    SegmentedButton(
                        selected = desiredOverlay == listType.ordinal,
                        onClick = {
                            desiredOverlay = listType.ordinal
                            Preferences.Enumerations.encodeValue(
                                DESIRED_OVERLAY, listType
                            )
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
        })
    }
}

enum class LayoutType(val icon: ImageVector) {
    Grid(icon = Icons.Rounded.GridView), List(icon = Icons.AutoMirrored.Rounded.List);

    companion object {
        fun Int.toListType(): LayoutType = entries.first { it.ordinal == this }
    }
}