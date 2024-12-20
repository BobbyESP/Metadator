package com.bobbyesp.metadator.presentation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.bobbyesp.metadator.domain.model.ParcelableSong
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
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.parcelableType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.typeOf

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun Navigator() {
    val navController = LocalNavController.current
    val drawerState = LocalDrawerState.current

    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentNavigator by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.parent?.route
        }
    }

    val currentRoute by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.route
        }
    }

    val snackbarHostState = LocalSnackbarHostState.current

    val mediaStoreViewModel = koinViewModel<MediaStorePageViewModel>()
    val mediaplayerViewModel = koinViewModel<MediaplayerViewModel>()

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

        val targetBottom by remember {
            derivedStateOf {
                if (!mediaPlayerSheetState.isDismissed) {
                    CollapsedPlayerHeight + bottomInset
                } else {
                    bottomInset
                }
            }
        }

        val animatedBottom by animateDpAsState(
            targetValue = targetBottom,
            label = "Animated bottom insets for player sheet"
        )

        val canDrawerBeOpened by remember {
            derivedStateOf {
                mainNavigators.fastAny { it.qualifiedName() == currentNavigator }
            }
        }

        val playerAwareWindowInsets by remember {
            derivedStateOf {
                windowsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(bottom = animatedBottom))
            }
        }

        CompositionLocalProvider(
            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets
        ) {
            ModalNavigationDrawerWrapper(
                modifier = Modifier,
                drawerState = drawerState,
                gesturesEnabled = canDrawerBeOpened,
                currentNavigator = currentNavigator,
                navController = navController,
                scope = scope,
            ) {
                Scaffold(
                    modifier = Modifier.windowInsetsPadding(
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

                            dialog<Route.MetadatorNavigator.Home.VisualSettings> {
                                Text("Dialog")
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

            val viewModel = koinViewModel<MetadataEditorVM>()
            val bsViewModel = koinViewModel<MetadataBsVM>()

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