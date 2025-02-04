package com.bobbyesp.metadator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaplayerService
import com.bobbyesp.metadator.core.data.local.preferences.AppPreferences
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.COMPLETED_ONBOARDING
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences.Companion.emptyUserPreferences
import com.bobbyesp.metadator.core.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.presentation.theme.MetadatorTheme
import com.bobbyesp.metadator.mediaplayer.data.local.MediaplayerServiceConnection
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
import org.koin.core.component.KoinComponent
import setCrashlyticsCollection

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity(), KoinComponent {
    private var isMusicPlayerServiceStarted = false
    private var startDestination: Route? = null

    private val connectionHandler: ConnectionHandler by inject()
    private val appPreferences: AppPreferences by inject()
    private val imageLoader: ImageLoader by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()

        lifecycleScope.launch(Dispatchers.IO) {
            val completedOnboarding =
                async { appPreferences.getSetting(COMPLETED_ONBOARDING, false) }
            startDestination = if (completedOnboarding.await()) {
                Route.MetadatorNavigator
            } else {
                Route.OnboardingNavigator
            }
        }

        splashscreen.setKeepOnScreenCondition {
            startDestination == null
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setCrashlyticsCollection()
        setContent {
            val navController = rememberNavController()
            val sonner = rememberToasterState()

            val userPreferences =
                appPreferences.userPreferencesFlow.collectAsStateWithLifecycle(
                    emptyUserPreferences()
                )

            CompositionLocalProvider(
                LocalNavController provides navController,
            ) {
                KoinContext {
                    val windowSizeClass = calculateWindowSizeClass(this)

                    AppLocalSettingsProvider(
                        windowWidthSize = windowSizeClass.widthSizeClass,
                        playerConnectionHandler = connectionHandler,
                        sonner = sonner,
                        appPreferences = appPreferences,
                        imageLoader = imageLoader
                    ) {
                        MetadatorTheme {
                            Navigator(
                                navController = navController,
                                startDestination = startDestination
                                    ?: throw IllegalStateException("Start destination couldn't be determinate"),
                                preferences = userPreferences
                            )
                            Toaster(
                                state = sonner,
                                richColors = true,
                                darkTheme = userPreferences.value.darkThemePreference.isDarkTheme()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMediaPlayerService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        isMusicPlayerServiceStarted = false
    }

    private fun startMediaPlayerService() {
        val intent = Intent(this, MediaplayerService::class.java)
        if (!isMusicPlayerServiceStarted) {
            isMusicPlayerServiceStarted = true
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    private var serviceConnection = MediaplayerServiceConnection(connectionHandler)
}