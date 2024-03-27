package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.bobbyesp.metadator.model.SelectedSong
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.NavArgs
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.common.TagEditorSelectedSongParamType
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.home.HomePage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPageViewModel
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.ui.util.appBarScrollBehavior
import com.bobbyesp.utilities.navigation.getParcelable

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint(
    "UnusedBoxWithConstraintsScope"
)
@Composable
fun Navigator() {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRootRoute = rememberSaveable(navBackStackEntry, key = "currentRootRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.parent?.route ?: Route.MetadatorNavigator.route
        )
    }
    val currentRoute = rememberSaveable(navBackStackEntry, key = "currentRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.route ?: Route.MetadatorNavigator.Home.route
        )
    }

    val snackbarHostState = LocalSnackbarHostState.current

    val showSnackbarMessage: suspend (String) -> Unit = { message ->
        snackbarHostState.showSnackbar(message)
    }

    val mediaStoreViewModel = hiltViewModel<MediaStorePageViewModel>()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            navController = navController,
            startDestination = Route.MetadatorNavigator.route,
            route = Route.MainHost.route,
        ) {
            navigation(
                startDestination = Route.MetadatorNavigator.Home.route,
                route = Route.MetadatorNavigator.route
            ) {
                animatedComposable(Route.MetadatorNavigator.Home.route) {
                    HomePage(viewModel = mediaStoreViewModel)
                }
            }

            navigation(
                startDestination = Route.UtilitiesNavigator.TagEditor.route,
                route = Route.UtilitiesNavigator.route
            ) {
                slideInVerticallyComposable(
                    route = Route.UtilitiesNavigator.TagEditor.route,
                    arguments = listOf(navArgument(NavArgs.TagEditorSelectedSong.key) {
                        type = TagEditorSelectedSongParamType
                    })
                ) {
                    val selectedSongParcelable =
                        it.getParcelable<SelectedSong>(NavArgs.TagEditorSelectedSong.key)

                    val viewModel = hiltViewModel<ID3MetadataEditorPageViewModel>()

                    ID3MetadataEditorPage(
                        viewModel = viewModel, selectedSong = selectedSongParcelable!!
                    )
                }
            }
        }
    }
}