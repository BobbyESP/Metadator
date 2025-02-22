package com.bobbyesp.crashhandler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.crashhandler.ui.CrashReportPage
import com.bobbyesp.crashhandler.ui.theme.CrashHandlerTheme

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
            val clipboardManager = LocalClipboardManager.current

            CrashHandlerTheme {
                CrashReportPage(
                    versionReport = versionReport,
                    errorMessage = log,
                    reportUrl = reportUrl,
                    onExitPressed = {
                        clipboardManager.setText(
                            AnnotatedString(
                                viewModel.generateReportToSend(versionReport)
                            )
                        )
                        this.finishAffinity()
                    }
                )
            }
        }
    }
}