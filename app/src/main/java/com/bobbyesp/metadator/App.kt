package com.bobbyesp.metadator

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.bobbyesp.crashhandler.CrashHandler.setupCrashHandler
import com.bobbyesp.crashhandler.ReportInfo
import com.bobbyesp.metadator.di.appCoroutinesScope
import com.bobbyesp.metadator.di.appMainViewModels
import com.bobbyesp.metadator.di.appSystemManagers
import com.bobbyesp.metadator.di.mediaplayerViewModels
import com.bobbyesp.metadator.di.utilitiesViewModels
import com.bobbyesp.metadator.features.spotify.di.spotifyMainModule
import com.bobbyesp.metadator.features.spotify.di.spotifyServicesModule
import com.bobbyesp.metadator.util.preferences.PreferencesKeys
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.BooleanPreferenceDefaults
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.IntPreferenceDefaults
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.StringPreferenceDefaults
import com.bobbyesp.utilities.Preferences
import com.tencent.mmkv.MMKV
import mediaplayerInternalsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import kotlin.properties.Delegates

class App : Application() {
    override fun onCreate() {
        MMKV.initialize(this)
        preferences = Preferences(
            PreferencesKeys.MMKV_PREFERENCES_NAME,
            IntPreferenceDefaults,
            StringPreferenceDefaults,
            BooleanPreferenceDefaults
        )
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appSystemManagers, appCoroutinesScope)
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
        lateinit var preferences: Preferences
        lateinit var packageInfo: PackageInfo
        var isPlayStoreBuild by Delegates.notNull<Boolean>()

        val appVersion: String get() = packageInfo.versionName.toString()

        const val APP_PACKAGE_NAME = "com.bobbyesp.metadator"
        const val APP_FILE_PROVIDER = "$APP_PACKAGE_NAME.fileprovider"
    }
}
