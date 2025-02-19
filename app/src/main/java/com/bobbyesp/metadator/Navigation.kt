package com.bobbyesp.metadator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.COMPLETED_ONBOARDING
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences
import com.bobbyesp.metadator.core.data.local.preferences.datastore.rememberPreferenceState
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.presentation.settingsRouting
import com.bobbyesp.metadator.core.util.cleanNavigate
import com.bobbyesp.metadator.core.util.navigateBack
import com.bobbyesp.metadator.mediaplayer.mediaplayerRouting
import com.bobbyesp.metadator.mediastore.presentation.MediaStorePageViewModel
import com.bobbyesp.metadator.mediastore.presentation.pages.home.HomePage
import com.bobbyesp.metadator.onboarding.onboardingRouting
import com.bobbyesp.metadator.tageditor.tagEditorRouting
import com.bobbyesp.ui.motion.animatedComposable
import org.koin.androidx.compose.koinViewModel

@Composable
fun Navigator(
    navController: NavHostController,
    startDestination: Route,
    preferences: State<UserPreferences>,
) {
    val mediaStoreViewModel = koinViewModel<MediaStorePageViewModel>()

    val (_, setOnboardingCompleted) = rememberPreferenceState(COMPLETED_ONBOARDING)

    NavHost(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
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

        mediaplayerRouting(
            //mediaplayerViewModel = mediaplayerViewModel,
            onNavigateBack = {
                navController.navigateBack()
            }
        )

        tagEditorRouting { navController.navigateBack() }
        settingsRouting { navController.navigateBack() }
    }
}

