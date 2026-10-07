package com.bobbyesp.metadator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorTheme
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.UserSettings
import com.bobbyesp.metadator.core.ui.viewmodel.LocalSnackbarHostState
import com.bobbyesp.metadator.library.mediastore.ActivityResultHost
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()
    private val resultHost: ActivityResultHost by inject()

    /** The splash stays until the theme is known, so the first frame is already the right one. */
    private val settingsLoaded = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !settingsLoaded.value }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Lends this activity's launchers for permission and write-access prompts.
        resultHost.attach(this)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            LaunchedEffect(settings != null) { if (settings != null) settingsLoaded.value = true }
            val current = settings ?: UserSettings()
            ReviewPrompt(successfulSaves = current.successfulSaves)

            val snackbar = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
                MetadatorTheme(current) { MetadatorApp(onExit = ::finish) }
            }
        }
    }
}
