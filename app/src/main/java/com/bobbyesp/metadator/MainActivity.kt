package com.bobbyesp.metadator

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.mediaplayer.service.MediaplayerService
import com.bobbyesp.metadator.presentation.Navigator
import com.bobbyesp.metadator.presentation.common.AppLocalSettingsProvider
import com.bobbyesp.metadator.presentation.theme.MetadatorTheme
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
import setCrashlyticsCollection

@androidx.annotation.OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    private var isMusicPlayerServiceStarted = false

    private val connectionHandler: ConnectionHandler by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        activity = this
        setCrashlyticsCollection()
        setContent {
            KoinContext {
                val windowSizeClass = calculateWindowSizeClass(this)
                AppLocalSettingsProvider(windowSizeClass.widthSizeClass, connectionHandler) {
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

    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(
                "MainActivity",
                "The Music Service is connected. Updating the connection handler."
            )
            val binder = service as MediaplayerService.MusicBinder
            connectionHandler.connect(binder.service.mediaServiceHandler)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(
                "MainActivity",
                "The Music Service has been disconnected. Detaching the connection handler."
            )
            connectionHandler.disconnect()
        }
    }

    companion object {
        private lateinit var activity: MainActivity
        fun getActivity(): MainActivity = activity
    }
}