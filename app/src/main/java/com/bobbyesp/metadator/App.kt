package com.bobbyesp.metadator

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.getSystemService
import com.bobbyesp.crashhandler.CrashHandler.setupCrashHandler
import com.bobbyesp.crashhandler.ReportInfo
import com.bobbyesp.utilities.Theme
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        MMKV.initialize(this)
        packageInfo = packageManager.run {
            if (Build.VERSION.SDK_INT >= 33) getPackageInfo(
                packageName, PackageManager.PackageInfoFlags.of(0)
            ) else
                getPackageInfo(packageName, 0)
        }
        context = applicationContext
        applicationScope = CoroutineScope(SupervisorJob())
        clipboard = getSystemService()!!
        connectivityManager = getSystemService()!!
        Theme.applicationScope = applicationScope
        isPlayStoreBuild = BuildConfig.FLAVOR == "playstore"
        DynamicColors.applyToActivitiesIfAvailable(this)
        super.onCreate()

        if(!isPlayStoreBuild) setupCrashHandler(reportInfo = ReportInfo(
            androidVersion = true,
            deviceInfo = true,
            supportedABIs = true
        ))
    }

    companion object {
        @ApplicationContext
        lateinit var context: Context
        lateinit var clipboard: ClipboardManager
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager
        lateinit var packageInfo: PackageInfo
        var isPlayStoreBuild by Delegates.notNull<Boolean>()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        const val APP_PACKAGE_NAME = "com.bobbyesp.metadator"
        const val APP_FILE_PROVIDER = "$APP_PACKAGE_NAME.fileprovider"
    }
}
