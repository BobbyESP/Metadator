package com.bobbyesp.metadator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.bobbyesp.mediaplayer.service.MediaplayerService
import com.bobbyesp.metadator.presentation.Navigator
import com.bobbyesp.metadator.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.presentation.common.LocalDarkTheme
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import dagger.hilt.android.AndroidEntryPoint
import setupFirebase

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var isMusicPlayerServiceStarted = false

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
        startMediaPlayerService()
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

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MediaplayerService::class.java))
        isMusicPlayerServiceStarted = false
    }

    fun startMediaPlayerService() {
        if (!isMusicPlayerServiceStarted) {
            isMusicPlayerServiceStarted = true
            startService(Intent(this, MediaplayerService::class.java))
        }
    }

    companion object {
        private lateinit var activity: MainActivity
        fun getActivity(): MainActivity {
            return activity
        }
    }
}