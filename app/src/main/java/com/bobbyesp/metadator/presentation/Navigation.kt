package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.bobbyesp.metadator.App
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.DestinationInfo
import com.bobbyesp.metadator.presentation.common.LocalDrawerState
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalPlayerAwareWindowInsets
import com.bobbyesp.metadator.presentation.common.LocalSnackbarHostState
import com.bobbyesp.metadator.presentation.common.Route
import com.bobbyesp.metadator.presentation.common.mainNavigators
import com.bobbyesp.metadator.presentation.common.qualifiedName
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.home.HomePage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerPage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.pages.mediaplayer.player.CollapsedPlayerHeight
import com.bobbyesp.metadator.presentation.pages.mediaplayer.player.PlayerAnimationSpec
import com.bobbyesp.metadator.presentation.pages.settings.SettingsPage
import com.bobbyesp.metadator.presentation.pages.settings.modules.GeneralSettingsPage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.MetadataEditorPage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.MetadataEditorVM
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.MetadataBsVM
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.components.tags.RoundedTag
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.parcelableType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Navigator() {
    val navController = LocalNavController.current
    val drawerState = LocalDrawerState.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentNavigator = rememberSaveable(navBackStackEntry, key = "currentRootRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.parent?.route
        )
    }
    val currentRoute = rememberSaveable(navBackStackEntry, key = "currentRoute") {
        mutableStateOf(
            navBackStackEntry?.destination?.route
        )
    }

    val snackbarHostState = LocalSnackbarHostState.current

    val scope = rememberCoroutineScope()

    //able to open drawer when the user is in one of the main routes (root routes)
    val canOpenDrawer by remember(currentRoute) {
        mutableStateOf(mainNavigators.fastAny { navigator -> navigator.qualifiedName() == currentNavigator.value })
    }

    val mediaStoreViewModel = hiltViewModel<MediaStorePageViewModel>()
    val mediaplayerViewModel = hiltViewModel<MediaplayerViewModel>()

    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            targetValue = targetBottom, label = "Animated bottom insets for player sheet"
        )

        val playerAwareWindowInsets = remember(
            bottomInset, mediaPlayerSheetState.isDismissed, animatedBottom
        ) {
            val insetsBottom = animatedBottom
            windowsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .add(WindowInsets(bottom = insetsBottom))
        }

        CompositionLocalProvider(
            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets
        ) {
            ModalNavigationDrawer(
                modifier = Modifier,
                drawerState = drawerState,
                gesturesEnabled = canOpenDrawer,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerState = drawerState,
                        modifier = Modifier,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                letterSpacing = 4.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .verticalScroll(
                                    rememberScrollState()
                                )
                        ) {
                            mainNavigators.forEach { route ->
                                val actualNavigator = route.qualifiedName()
//                                val isSelected by remember(currentNavigator, actualNavigator) {
//                                    mutableStateOf(currentNavigator.value == actualNavigator)
//                                }
                                val isSelected = currentNavigator.value == actualNavigator
                                val destinationInfo = DestinationInfo.fromRoute(route)

                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = stringResource(
                                                id = destinationInfo?.title ?: R.string.unknown
                                            )
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        } else {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
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
                                        Icon(imageVector = destinationInfo?.icon
                                            ?: Icons.Rounded.Square,
                                            contentDescription = destinationInfo?.title?.let {
                                                stringResource(id = it)
                                            })
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier, verticalAlignment = Alignment.CenterVertically
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
                            Surface(onClick = {
                                navController.navigate(Route.SettingsNavigator.Settings)
                                scope.launch {
                                    drawerState.close()
                                }
                            },
                                modifier = Modifier
                                    .semantics { role = Role.Tab }
                                    .height(56.dp)
                                    .fillMaxWidth(),
                                color = Color.Transparent) {
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
                Scaffold(modifier = Modifier.windowInsetsPadding(
                    insets = WindowInsets(
                        left = WindowInsets.safeDrawing.getLeft(
                            density, layoutDirection = LocalLayoutDirection.current
                        ),
                        right = WindowInsets.safeDrawing.getRight(
                            density, layoutDirection = LocalLayoutDirection.current
                        ),
                    )
                ), snackbarHost = {
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
                        route = Route.MainHost::class,
                        startDestination = Route.MetadatorNavigator,
                    ) {
                        navigation<Route.MetadatorNavigator>(
                            startDestination = Route.MetadatorNavigator.Home,
                        ) {
                            animatedComposable<Route.MetadatorNavigator.Home> {
                                val songsState =
                                    mediaStoreViewModel.songs.collectAsStateWithLifecycle()
                                HomePage(songs = songsState, onEvent = mediaStoreViewModel::onEvent)
                            }
                        }

                        navigation<Route.MediaplayerNavigator>(
                            startDestination = Route.MediaplayerNavigator.Mediaplayer,
                        ) {
                            animatedComposable<Route.MediaplayerNavigator.Mediaplayer> {
                                MediaplayerPage(mediaplayerViewModel, mediaPlayerSheetState)
                            }
                        }

                        utilitiesNavigation { navController.popBackStack() }
                        settingsNavigation { navController.popBackStack() }
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.utilitiesNavigation(
    onNavigateBack: () -> Unit
) {
    navigation<Route.UtilitiesNavigator>(
        startDestination = Route.UtilitiesNavigator.TagEditor::class,
    ) {
        slideInVerticallyComposable<Route.UtilitiesNavigator.TagEditor>(
            typeMap = mapOf(typeOf<ParcelableSong>() to parcelableType<ParcelableSong>()),
        ) {
            val song = it.toRoute<Route.UtilitiesNavigator.TagEditor>()

            val viewModel = hiltViewModel<MetadataEditorVM>()
            val bsViewModel = hiltViewModel<MetadataBsVM>()

            val state = viewModel.state.collectAsStateWithLifecycle()
            val bsState = bsViewModel.viewStateFlow.collectAsStateWithLifecycle()
            val securityErrorHandler =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.savePropertyMap(
                            audioPath = song.selectedSong.localPath
                        )
                        onNavigateBack()
                    }
                }

            LaunchedEffect(true) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is MetadataEditorVM.UiEvent.RequestPermission -> {
                            val intent =
                                IntentSenderRequest.Builder(event.intent)
                                    .build()
                            securityErrorHandler.launch(intent)
                        }

                        is MetadataEditorVM.UiEvent.SaveSuccess -> {
                            onNavigateBack()
                        }
                    }
                }
            }

            LaunchedEffect(true) {
                bsViewModel.outerEventsFlow.collectLatest { event ->
                    when (event) {
                        is MetadataBsVM.OuterEvent.SaveMetadata -> {
                            event.modifiedFields.forEach { field ->
                                viewModel.onEvent(
                                    MetadataEditorVM.Event.UpdateProperty(
                                        field.key,
                                        field.value
                                    )
                                )
                            }
                        }
                    }
                }
            }

            MetadataEditorPage(
                state = state,
                bsViewState = bsState,
                receivedAudio = song.selectedSong,
                onBsEvent = bsViewModel::onEvent,
                onEvent = viewModel::onEvent
            )
        }
    }
}

fun NavGraphBuilder.settingsNavigation(
    onNavigateBack: () -> Unit
) {
    navigation<Route.SettingsNavigator>(
        startDestination = Route.SettingsNavigator.Settings,
    ) {
        animatedComposable<Route.SettingsNavigator.Settings> {
            SettingsPage(
                onBackPressed = onNavigateBack
            )
        }

        animatedComposable<Route.SettingsNavigator.Settings.General> {
            GeneralSettingsPage()
        }

        animatedComposable<Route.SettingsNavigator.Settings.Appearance> {
            Text("Appearance")
        }

        animatedComposable<Route.SettingsNavigator.Settings.About> {
            Text("About")
        }
    }
}