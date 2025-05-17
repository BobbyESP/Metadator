package com.bobbyesp.crashhandler

import android.content.ClipData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.crashhandler.ui.CrashReportPage
import com.bobbyesp.crashhandler.ui.theme.CrashHandlerTheme
import kotlinx.coroutines.launch

class CrashHandlerActivity : ComponentActivity() {
    private val viewModel: CrashHandlerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionReport: String = intent.getStringExtra("version_report").toString()
        val logfilePath: String = intent.getStringExtra("logfile_path").toString()
        val reportUrl: String = intent.getStringExtra("report_url").toString()

        viewModel.loadLog(logfilePath)

        setContent {
            val log by viewModel.log.collectAsStateWithLifecycle()
            val clipboard = LocalClipboard.current

            val scope = rememberCoroutineScope()

            CrashHandlerTheme {
                CrashReportPage(
                    versionReport = versionReport,
                    errorMessage = log,
                    reportUrl = reportUrl,
                    onExitPressed = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        "Crash Report", AnnotatedString(
                                            viewModel.generateReportToSend(versionReport)
                                        )
                                    )
                                )
                            )
                        }
                        this.finishAffinity()
                    })
            }
        }
    }
}