package com.bobbyesp.metadator

import android.app.Application
import android.content.ClipboardManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.getSystemService
import com.bobbyesp.crashhandler.CrashHandler.setupCrashHandler
import com.bobbyesp.crashhandler.ReportInfo
import com.bobbyesp.metadator.di.appMainViewModels
import com.bobbyesp.metadator.di.mediaplayerViewModels
import com.bobbyesp.metadator.di.utilitiesViewModels
import com.bobbyesp.metadator.features.spotify.di.spotifyMainModule
import com.bobbyesp.metadator.features.spotify.di.spotifyServicesModule
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import mediaplayerInternalsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import kotlin.properties.Delegates

class App : Application() {
    override fun onCreate() {
        MMKV.initialize(this)
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(mediaplayerInternalsModule)
            modules(appMainViewModels, utilitiesViewModels, mediaplayerViewModels)
            modules(spotifyMainModule, spotifyServicesModule)
        }
        packageInfo = packageManager.run {
            if (Build.VERSION.SDK_INT >= 33) getPackageInfo(
                packageName, PackageManager.PackageInfoFlags.of(0)
            ) else
                getPackageInfo(packageName, 0)
        }
        applicationScope = CoroutineScope(SupervisorJob())
        clipboard = getSystemService()!!
        connectivityManager = getSystemService()!!
        isPlayStoreBuild = BuildConfig.FLAVOR == "playstore"
        super.onCreate()

        if (!isPlayStoreBuild) setupCrashHandler(
            reportInfo = ReportInfo(
                androidVersion = true,
                deviceInfo = true,
                supportedABIs = true
            )
        )
    }

    companion object {
        lateinit var clipboard: ClipboardManager
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager
        lateinit var packageInfo: PackageInfo
        var isPlayStoreBuild by Delegates.notNull<Boolean>()

        val appVersion: String get() = packageInfo.versionName.toString()

        const val APP_PACKAGE_NAME = "com.bobbyesp.metadator"
        const val APP_FILE_PROVIDER = "$APP_PACKAGE_NAME.fileprovider"
    }
}
