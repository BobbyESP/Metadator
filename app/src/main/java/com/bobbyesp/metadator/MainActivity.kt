package com.bobbyesp.metadator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.bobbyesp.metadator.presentation.Navigator
import com.bobbyesp.metadator.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.presentation.common.LocalDarkTheme
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import dagger.hilt.android.AndroidEntryPoint
import setupFirebase

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }
        activity = this
        setupFirebase()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppLocalSettingsProvider(windowSizeClass.widthSizeClass) {
                MetadatorTheme(
                    darkTheme = LocalDarkTheme.current.isDarkTheme(),
                    isHighContrastModeEnabled = LocalDarkTheme.current.isHighContrastModeEnabled,
                ) {
                    Navigator()
                }
            }
        }
    }

    companion object {
        private lateinit var activity: MainActivity
        const val CLOSE_ACTIVITY_NON_GRANTED_PERMISSIONS = 1
        fun getActivity(): MainActivity {
            return activity
        }
    }
}