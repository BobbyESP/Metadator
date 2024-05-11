package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.bobbyesp.metadator.App
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalDrawerState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalPlayerAwareWindowInsets
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.NavArgs
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.common.TagEditorParcelableSongParamType
import com.bobbyesp.metadator.presentation.common.routesToNavigate
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.home.HomePage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerPage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer.CollapsedPlayerHeight
import com.bobbyesp.metadator.presentation.pages.mediaplayer.mediaplayer.PlayerAnimationSpec
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.ID3MetadataEditorPageViewModel
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.components.tags.RoundedTag
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.getParcelable
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()

    //able to open drawer when the user is in one of the main routes (root routes)
    val canOpenDrawer by remember(currentRoute) {
        mutableStateOf(routesToNavigate.fastAny { it.route == currentRootRoute.value })
    }

    val mediaStoreViewModel = hiltViewModel<MediaStorePageViewModel>()
    val mediaplayerViewModel = hiltViewModel<MediaplayerViewModel>()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val density = LocalDensity.current
        val windowsInsets = WindowInsets.systemBars

        val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
        val mediaPlayerSheetState = rememberDraggableBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = bottomInset + CollapsedPlayerHeight,
            expandedBound = maxHeight,
            animationSpec = PlayerAnimationSpec,
        )

        val targetBottom = if (!mediaPlayerSheetState.isDismissed) {
            CollapsedPlayerHeight + bottomInset
        } else {
            bottomInset
        }

        val animatedBottom by animateDpAsState(
            targetValue = targetBottom,
            label = "Animated bottom insets for player sheet"
        )

        val playerAwareWindowInsets = remember(
            bottomInset,
            mediaPlayerSheetState.isDismissed,
            animatedBottom
        ) {
            val insetsBottom = animatedBottom
            windowsInsets
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .add(WindowInsets(bottom = insetsBottom))
        }


        CompositionLocalProvider(
            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets
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
                        routesToNavigate.forEachIndexed { _, route ->
                            val isSelected = currentRootRoute.value == route.route
                            NavigationDrawerItem(
                                label = {
                                    Text(text = route.title?.let { stringResource(id = it) } ?: "")
                                },
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        scope.launch {
                                            drawerState.close()
                                        }
                                        return@NavigationDrawerItem
                                    } else {
                                        navController.navigate(route.route) {
                                            popUpTo(Route.MainHost.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = route.icon ?: Icons.Rounded.Square,
                                        contentDescription = route.title?.let { stringResource(id = it) })
                                },
                                badge = {

                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.padding(16.dp),
                                    text = stringResource(id = R.string.app_name).uppercase() + " " + App.appVersion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    overflow = TextOverflow.Ellipsis
                                )
                                RoundedTag(
                                    text = if (App.isPlayStoreBuild) "PLAY STORE" else "FOSS",
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                            Surface(
                                onClick = {
                                    //TODO: Open settings
                                },
                                modifier = Modifier
                                    .semantics { role = Role.Tab }
                                    .height(56.dp)
                                    .fillMaxWidth(),
                                color = Color.Transparent
                            ) {
                                Row(
                                    Modifier.padding(start = 16.dp, end = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(id = R.string.settings)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
            ) {
                Scaffold(snackbarHost = {
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
                }) {
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
                            startDestination = Route.MediaplayerNavigator.Mediaplayer.route,
                            route = Route.MediaplayerNavigator.route
                        ) {
                            animatedComposable(Route.MediaplayerNavigator.Mediaplayer.route) {
                                MediaplayerPage(mediaplayerViewModel, mediaPlayerSheetState)
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
                                    viewModel = viewModel,
                                    parcelableSong = parcelableSongParcelable!!
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}