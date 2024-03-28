package com.bobbyesp.metadator.presentation.pages.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalDrawerState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.pages.MediaStorePage
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.utilities.ui.permission.PermissionNotGrantedDialog
import com.bobbyesp.utilities.ui.permission.PermissionRequestHandler
import com.bobbyesp.utilities.ui.permission.toPermissionType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier, viewModel: MediaStorePageViewModel
) {
    val currentApiVersion = Build.VERSION.SDK_INT
    val readAudioFiles = when {
        currentApiVersion < Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_EXTERNAL_STORAGE

        else -> Manifest.permission.READ_MEDIA_AUDIO
    }

    val storagePermissionState = rememberPermissionState(permission = readAudioFiles)
    val navController = LocalNavController.current
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
            },
            title = {
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
                    Text(
                        text = stringResource(id = R.string.app_desc).uppercase(),
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }, scrollBehavior = scrollBehavior
        )
    }) { paddingValues ->
        PermissionRequestHandler(permissionState = storagePermissionState,
            deniedContent = { shouldShowRationale ->
                PermissionNotGrantedDialog(
                    neededPermissions = listOf(readAudioFiles.toPermissionType()),
                    onGrantRequest = {
                        storagePermissionState.launchPermissionRequest()
                    },
                    onDismissRequest = {
                        navController.popBackStack()
                    },
                    shouldShowRationale = shouldShowRationale
                )
            },
            content = {
                MediaStorePage(modifier = Modifier.padding(paddingValues = paddingValues),
                    viewModel = viewModel,
                    onItemClicked = { song ->
                        val artistsList = song.artist.toList()
                        val mainArtist = artistsList.first().toString()

                        val chosenSongParcel = ParcelableSong(
                            name = song.title,
                            mainArtist = mainArtist,
                            localSongPath = song.path,
                            artworkPath = song.albumArtPath,
                            fileName = song.fileName
                        )

                        navController.navigate(
                            Route.UtilitiesNavigator.TagEditor.createRoute(chosenSongParcel)
                        )
                    })
            })
    }
}