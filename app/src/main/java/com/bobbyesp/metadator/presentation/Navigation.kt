package com.bobbyesp.metadator.presentation

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.COMPLETED_ONBOARDING
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreferenceState
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.LocalPlayerAwareWindowInsets
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.util.cleanNavigate
import com.bobbyesp.metadator.core.util.navigateBack
import com.bobbyesp.metadator.core.presentation.pages.settings.SettingsPage
import com.bobbyesp.metadator.core.presentation.pages.settings.modules.GeneralSettingsPage
import com.bobbyesp.metadator.domain.model.ParcelableSong
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerPage
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.CollapsedPlayerHeight
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.PlayerAnimationSpec
import com.bobbyesp.metadator.onboarding.onboardingNavigation
import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.home.HomePage
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.MetadataEditorPage
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.MetadataEditorViewModel
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.parcelableType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.typeOf

@OptIn(ExperimentalMaterial3ExpressiveApi::class)

@Composable
fun Navigator(
    startDestination: Route,
    preferences: State<UserPreferences>,
) {
    val navController = LocalNavController.current

    val mediaStoreViewModel = koinViewModel<MediaStorePageViewModel>()
    val mediaplayerViewModel = koinViewModel<MediaplayerViewModel>()

    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars

    val (_, setOnboardingCompleted) = rememberPreferenceState(COMPLETED_ONBOARDING)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
        val mediaPlayerSheetState = rememberDraggableBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = bottomInset + CollapsedPlayerHeight,
            expandedBound = this.maxHeight,
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

        val playerAwareWindowInsets by remember {
            derivedStateOf {
                windowsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(bottom = animatedBottom))
            }
        }

        CompositionLocalProvider(
            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets
        ) {
            NavHost(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                navController = navController,
                startDestination = startDestination,
            ) {
                onboardingNavigation(
                    onNavigate = { navController.navigate(it) },
                    onCompletedOnboarding = {
                        setOnboardingCompleted(true)
                        navController.cleanNavigate(Route.MetadatorNavigator)
                    }
                )

                navigation<Route.MetadatorNavigator>(
                    startDestination = Route.MetadatorNavigator.Home,
                ) {
                    animatedComposable<Route.MetadatorNavigator.Home> {
                        val songsState =
                            mediaStoreViewModel.songs.collectAsStateWithLifecycle()
                        HomePage(
                            songs = songsState,
                            preferences = preferences,
                            onEvent = mediaStoreViewModel::onEvent
                        )
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

                utilitiesNavigation { navController.navigateBack() }
                settingsNavigation { navController.navigateBack() }
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

            val viewModel = koinViewModel<MetadataEditorViewModel>()
            val bsViewModel = koinViewModel<MetadataBottomSheetViewModel>()

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
                        is MetadataEditorViewModel.UiEvent.RequestPermission -> {
                            val intent =
                                IntentSenderRequest.Builder(event.intent)
                                    .build()
                            securityErrorHandler.launch(intent)
                        }

                        is MetadataEditorViewModel.UiEvent.SaveSuccess -> {
                            onNavigateBack()
                        }
                    }
                }
            }

            LaunchedEffect(true) {
                bsViewModel.outerEventsFlow.collectLatest { event ->
                    when (event) {
                        is MetadataBottomSheetViewModel.OuterEvent.SaveMetadata -> {
                            event.modifiedFields.forEach { field ->
                                viewModel.onEvent(
                                    MetadataEditorViewModel.Event.UpdateProperty(
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