package com.bobbyesp.metadator.presentation.pages.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.SelectedSong
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.pages.MediaStorePage
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.utilities.ui.permission.PermissionNotGrantedDialog
import com.bobbyesp.utilities.ui.permission.PermissionRequestHandler
import com.bobbyesp.utilities.ui.permission.toPermissionType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier, viewModel: MediaStorePageViewModel
) {
    val currentApiVersion = Build.VERSION.SDK_INT

    val readAudioFiles = when {
        currentApiVersion < Build.VERSION_CODES.S -> Manifest.permission.READ_EXTERNAL_STORAGE

        else -> Manifest.permission.READ_MEDIA_AUDIO
    }

    val storagePermissionState = rememberPermissionState(permission = readAudioFiles)
    val navController = LocalNavController.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.app_name)
                )
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

                        val selectedSongParcel = SelectedSong(
                            name = song.title,
                            mainArtist = mainArtist,
                            localSongPath = song.path,
                            artworkPath = song.albumArtPath,
                            fileName = song.fileName
                        )

                        navController.navigate(
                            Route.UtilitiesNavigator.TagEditor.createRoute(selectedSongParcel)
                        )
                    })
            })
    }
}