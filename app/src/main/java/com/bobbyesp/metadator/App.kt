package com.bobbyesp.metadator

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.bobbyesp.crashhandler.CrashHandler.setupCrashHandler
import com.bobbyesp.crashhandler.ReportInfo
import com.bobbyesp.metadator.core.di.appCoroutinesScope
import com.bobbyesp.metadator.core.di.appSystemManagers
import com.bobbyesp.metadator.core.di.coreFunctionalitiesModule
import com.bobbyesp.metadator.features.spotify.di.spotifyMainModule
import com.bobbyesp.metadator.features.spotify.di.spotifyServicesModule
import com.bobbyesp.metadator.mediaplayer.di.mediaplayerViewModels
import com.bobbyesp.metadator.mediastore.di.mediaStoreViewModelsModule
import com.bobbyesp.metadator.tageditor.di.tagEditorModule
import com.bobbyesp.metadator.tageditor.di.tagEditorViewModelsModule
import kotlin.properties.Delegates
import mediaplayerInternalsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appSystemManagers, appCoroutinesScope, coreFunctionalitiesModule)
            modules(mediaplayerInternalsModule)
            modules(mediaStoreViewModelsModule, tagEditorViewModelsModule, mediaplayerViewModels)
            modules(tagEditorModule)
            modules(spotifyMainModule, spotifyServicesModule)
        }
        packageInfo =
            packageManager.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                else getPackageInfo(packageName, 0)
            }
        isPlayStoreBuild = (BuildConfig.FLAVOR == "playstore")
        super.onCreate()

        if (!isPlayStoreBuild)
            setupCrashHandler(
                reportInfo =
                    ReportInfo(androidVersion = true, deviceInfo = true, supportedABIs = true),
                reportUrl = CRASH_REPORT_URL,
            )
    }

    companion object {
        lateinit var packageInfo: PackageInfo
        var isPlayStoreBuild by Delegates.notNull<Boolean>()

        val appVersion: String
            get() = packageInfo.versionName.toString()

        const val APP_PACKAGE_NAME = "com.bobbyesp.metadator"
        const val PREFERENCES_NAME = "${APP_PACKAGE_NAME}_preferences"
        const val APP_FILE_PROVIDER = "$APP_PACKAGE_NAME.fileprovider"

        const val CRASH_REPORT_URL =
            "https://github.com/BobbyESP/Metadator/issues/new?assignees=&labels=bug&projects=&template=bug_report.yml&title=%5BApp%20crash%5D"
    }
}
