package com.bobbyesp.metadator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaplayerService
import com.bobbyesp.metadator.presentation.Navigator
import com.bobbyesp.metadator.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import com.bobbyesp.metadator.util.preferences.BooleanPreferenceDefaults
import com.bobbyesp.metadator.util.preferences.CoreSettings
import com.bobbyesp.metadator.util.preferences.IntPreferenceDefaults
import com.bobbyesp.metadator.util.preferences.PreferencesKeys
import com.bobbyesp.metadator.util.preferences.StringPreferenceDefaults
import com.bobbyesp.utilities.Preferences
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
import org.koin.core.component.KoinComponent
import setCrashlyticsCollection
import kotlin.getValue

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity(), KoinComponent {
    private var isMusicPlayerServiceStarted = false

    private val connectionHandler: ConnectionHandler by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setCrashlyticsCollection()
        setContent {
            KoinContext {
                val windowSizeClass = calculateWindowSizeClass(this)
                AppLocalSettingsProvider(
                    windowWidthSize = windowSizeClass.widthSizeClass,
                    playerConnectionHandler = connectionHandler,
                    coreSettings = CoreSettings(App.preferences.kv)
                ) {
                    MetadatorTheme {
                        Navigator()
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