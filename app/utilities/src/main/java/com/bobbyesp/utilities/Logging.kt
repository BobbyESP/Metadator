package com.bobbyesp.utilities

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import java.io.File

object Logging {
    private val callingClass = Throwable().stackTrace[1].className
    val isDebug = BuildConfig.DEBUG

    fun i(message: String) = Log.i(callingClass, message)

    fun d(message: String) = Log.d(callingClass, message)

    fun e(message: String) = Log.e(callingClass, message)

    fun e(throwable: Throwable) = Log.e(callingClass, throwable.message ?: "No message", throwable)

    fun e(message: String, throwable: Throwable) = Log.e(callingClass, message, throwable)

    fun w(message: String) = Log.w(callingClass, message)

    fun v(message: String) = Log.v(callingClass, message)

    fun wtf(message: String) = Log.wtf(callingClass, message)

    fun getVersionReport(packageInfo: PackageInfo): String {
        val versionName = packageInfo.versionName
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
            }
        val release =
            if (Build.VERSION.SDK_INT >= 30) {
                Build.VERSION.RELEASE_OR_CODENAME
            } else {
                Build.VERSION.RELEASE
            }
        val appName = packageInfo.applicationInfo?.name
        return """
            App version: $appName $versionName ($versionCode)
            Android version: Android $release (API ${Build.VERSION.SDK_INT})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Supported ABIs: ${Build.SUPPORTED_ABIS.contentToString()}
        """
            .trimIndent()
    }

    fun createLogFile(context: Context, errorReport: String): String {
        val date = Time.getZuluTimeSnapshot()
        val fileName = "log_$date.txt"
        val logFile =
            File(context.filesDir, fileName).apply {
                if (!exists()) createNewFile()
                appendText(errorReport)
            }
        return logFile.absolutePath
    }
}
