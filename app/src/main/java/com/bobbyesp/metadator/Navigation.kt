package com.bobbyesp.metadator

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.COMPLETED_ONBOARDING
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreferenceState
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.LocalPlayerAwareWindowInsets
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.util.cleanNavigate
import com.bobbyesp.metadator.core.util.navigateBack
import com.bobbyesp.metadator.core.presentation.settingsRouting
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerPage
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.CollapsedPlayerHeight
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.PlayerAnimationSpec
import com.bobbyesp.metadator.onboarding.onboardingRouting
import com.bobbyesp.metadator.mediastore.presentation.MediaStorePageViewModel
import com.bobbyesp.metadator.mediastore.presentation.pages.home.HomePage
import com.bobbyesp.metadator.tageditor.tagEditorRouting
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.motion.animatedComposable
import org.koin.androidx.compose.koinViewModel

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
                onboardingRouting(
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
                }

                navigation<Route.MediaplayerNavigator>(
                    startDestination = Route.MediaplayerNavigator.Mediaplayer,
                ) {
                    animatedComposable<Route.MediaplayerNavigator.Mediaplayer> {
                        MediaplayerPage(mediaplayerViewModel, mediaPlayerSheetState)
                    }
                }

                tagEditorRouting { navController.navigateBack() }
                settingsRouting { navController.navigateBack() }
            }
        }
    }
}

