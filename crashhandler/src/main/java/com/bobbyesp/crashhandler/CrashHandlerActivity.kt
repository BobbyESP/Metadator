package com.bobbyesp.crashhandler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.bobbyesp.crashhandler.ui.CrashReportPage
import com.bobbyesp.crashhandler.ui.theme.CrashHandlerTheme
import java.io.File

class CrashHandlerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionReport: String = intent.getStringExtra("version_report").toString()
        val logfilePath: String = intent.getStringExtra("logfile_path").toString()

        setContent {
            CrashHandlerTheme {
                val clipboardManager = LocalClipboardManager.current
                var log by rememberSaveable(key = "log") {
                    mutableStateOf("")
                }

                LaunchedEffect(true) {
                    val logFile = File(logfilePath)
                    log = logFile.readText()
                }

                CrashReportPage(
                    versionReport = versionReport,
                    errorMessage = log
                ) {
                    clipboardManager.setText(
                        AnnotatedString(versionReport).plus(
                            AnnotatedString(
                                "\n"
                            )
                        ).plus(AnnotatedString(log))
                    )
                    this.finishAffinity()
                }
            }
        }
    }
}