package com.bobbyesp.metadator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import com.bobbyesp.metadator.core.data.local.preferences.AppPreferences
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.COMPLETED_ONBOARDING
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences.Companion.emptyUserPreferences
import com.bobbyesp.metadator.core.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.presentation.theme.MetadatorTheme
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
    private var startDestination: Route? = null

    private val appPreferences: AppPreferences by inject()
    private val imageLoader: ImageLoader by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()

        lifecycleScope.launch(Dispatchers.IO) {
            val completedOnboarding = async {
                appPreferences.getSetting(COMPLETED_ONBOARDING, false)
            }
            startDestination =
                if (completedOnboarding.await()) {
                    Route.MetadatorNavigator
                } else {
                    Route.OnboardingNavigator
                }
        }

        splashscreen.setKeepOnScreenCondition { startDestination == null }

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

            CompositionLocalProvider(LocalNavController provides navController) {
                KoinContext {
                    val windowSizeClass = calculateWindowSizeClass(this)

                    AppLocalSettingsProvider(
                        windowWidthSize = windowSizeClass.widthSizeClass,
                        sonner = sonner,
                        appPreferences = appPreferences,
                        imageLoader = imageLoader,
                    ) {
                        MetadatorTheme {
                            Navigator(
                                navController = navController,
                                startDestination =
                                    startDestination
                                        ?: throw IllegalStateException(
                                            "Start destination couldn't be determinate"
                                        ),
                                preferences = userPreferences,
                            )
                            Toaster(
                                state = sonner,
                                richColors = true,
                                darkTheme = userPreferences.value.darkThemePreference.isDarkTheme(),
                            )
                        }
                    }
                }
            }
        }
    }
}
