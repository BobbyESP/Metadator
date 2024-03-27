package com.bobbyesp.crashhandler

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat.startActivity
import java.io.File

object CrashHandler {
    /**
     * This extension function is used to start the CrashReportActivity from any Activity.
     *
     * @param context The application context.
     * @param packageInfo The package information for which the report is to be generated.
     * @param reportInfo The report information which specifies what information should be included in the report. Defaults to a new instance of ReportInfo.
     * @param logfilePath The path of the log file to be passed to the CrashReportActivity.
     *
     * The function creates a new Intent for the CrashReportActivity and sets the necessary flags.
     * It then adds the version report and the log file path as extras to the Intent.
     * Finally, it starts the CrashReportActivity with the prepared Intent.
     */
    fun startCrashReportActivity(
        context: Context,
        packageInfo: PackageInfo,
        reportInfo: ReportInfo = ReportInfo(),
        logfilePath: String
    ) {
        startActivity(context, Intent(context, CrashHandlerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("version_report", getVersionReport(packageInfo, reportInfo))
            putExtra("logfile_path", logfilePath)
        }, null)
    }

    /**
     * This function sets up the crash handler for the application.
     *
     * @param reportInfo The report information which specifies what information should be included in the report. Defaults to a new instance of ReportInfo.
     *
     * The function sets the default uncaught exception handler to a new handler that creates a log file with the stack trace of the uncaught exception,
     * retrieves the package information for the application, and starts the CrashReportActivity with the generated log file and package information.
     *
     * The log file is created in the application's files directory and its name is generated using the current time in milliseconds.
     * The package information is retrieved using the package manager and includes the version name, version code, and package name.
     * The CrashReportActivity is started with a new Intent that includes the version report and the path of the log file as extras.
     */
    fun Application.setupCrashHandler(reportInfo: ReportInfo = ReportInfo()) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val logfile = createLogFile(this, throwable.stackTraceToString())
            val packageInfo = packageManager.run {
                if (Build.VERSION.SDK_INT >= 33) getPackageInfo(
                    packageName, PackageManager.PackageInfoFlags.of(0)
                ) else getPackageInfo(packageName, 0)
            }
            startCrashReportActivity(this, packageInfo, reportInfo, logfile)
        }
    }

    /**
     * This function generates a version report for the given package and report information.
     *
     * @param packageInfo The package information for which the report is to be generated.
     * @param info The report information which specifies what information should be included in the report.
     *
     * The function first retrieves the version name and version code from the package information.
     * It then determines the Android release version.
     *
     * A map is created where each key is a boolean condition from the report information and each value is the corresponding string to be appended to the report.
     * The function then iterates over the map, and for each entry, if the key (the condition) is true, the value (the string) is appended to the report.
     *
     * @return The generated version report as a string.
     */
    fun getVersionReport(packageInfo: PackageInfo, info: ReportInfo = ReportInfo()): String {
        val versionName = packageInfo.versionName

        @Suppress("DEPRECATION") val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }

        val androidVersion = if (Build.VERSION.SDK_INT >= 30) {
            Build.VERSION.RELEASE_OR_CODENAME
        } else {
            Build.VERSION.RELEASE
        }

        val packageName = packageInfo.packageName

        val report =
            StringBuilder().append("App version: $packageName $versionName ($versionCode)\n")


        if (info.androidVersion) {
            report.append("Android version: Android $androidVersion (API ${Build.VERSION.SDK_INT})\n")
        }

        if (info.deviceInfo) {
            report.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        }

        if (info.supportedABIs) {
            report.append("Supported ABIs: ${Build.SUPPORTED_ABIS.contentToString()}\n")
        }

        return report.toString() //It's only returned supportedABIs for some reason among the appended value at the creation of the "report" val
    }

    /**
     * This function is used to create a log file in the specified directory with the provided error report.
     * If the directory is not specified, it defaults to the application's files directory.
     *
     * @param context The application context.
     * @param errorReport The error report to be written to the log file.
     * @param directory The directory where the log file will be created. Defaults to the application's files directory.
     * @return The absolute path of the created log file.
     */
    fun createLogFile(
        context: Context, errorReport: String, directory: File = context.filesDir
    ): String {
        // Get the current time in milliseconds
        val date = System.currentTimeMillis()

        // Create a file name using the current time
        val fileName = "log_$date.txt"

        // Create a new file in the specified directory with the generated file name
        val logFile = File(directory, fileName)

        // Check if the file already exists, if not, create a new file
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        // Append the error report to the log file
        logFile.appendText(errorReport)

        // Return the absolute path of the log file
        return logFile.absolutePath
    }

}