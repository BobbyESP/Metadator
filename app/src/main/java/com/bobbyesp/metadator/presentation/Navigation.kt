package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalDrawerState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.NavArgs
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.common.TagEditorParcelableSongParamType
import com.bobbyesp.metadator.presentation.common.routesToNavigate
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.home.HomePage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPageViewModel
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.getParcelable

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Navigator() {
    val navController = LocalNavController.current
    val drawerState = LocalDrawerState.current

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

    //able to open drawer when the user is in one of the main routes (root routes)
    val canOpenDrawer by remember(currentRoute) {
        mutableStateOf(routesToNavigate.fastAny { it.route == currentRootRoute.value })
    }

    val mediaStoreViewModel = hiltViewModel<MediaStorePageViewModel>()

    LaunchedEffect(canOpenDrawer) {
        Log.i("Navigator", "canOpenDrawer: $canOpenDrawer")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = canOpenDrawer,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(id = R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            letterSpacing = 4.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    routesToNavigate.forEachIndexed { index, route ->
                        NavigationDrawerItem(label = {
                            Text(text = route.title?.let { stringResource(id = it) } ?: "")
                        }, selected = currentRootRoute.value == route.route, onClick = {

                        }, icon = {
                            Icon(imageVector = route.icon!!,
                                contentDescription = route.title?.let { stringResource(id = it) })
                        }, badge = {

                        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
        ) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState
                    ) { dataReceived ->
                        Snackbar(
                            modifier = Modifier,
                            snackbarData = dataReceived,
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
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
                                type = TagEditorParcelableSongParamType
                            })
                        ) {
                            val parcelableSongParcelable =
                                it.getParcelable<ParcelableSong>(NavArgs.TagEditorSelectedSong.key)

                            val viewModel = hiltViewModel<ID3MetadataEditorPageViewModel>()

                            ID3MetadataEditorPage(
                                viewModel = viewModel, parcelableSong = parcelableSongParcelable!!
                            )
                        }
                    }
                }
            }
        }
    }
}