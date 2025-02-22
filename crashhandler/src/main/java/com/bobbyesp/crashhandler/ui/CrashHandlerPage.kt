package com.bobbyesp.crashhandler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CancelScheduleSend
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bobbyesp.crashhandler.R
import com.bobbyesp.crashhandler.ui.components.ExpandableElevatedCard
import com.bobbyesp.crashhandler.ui.components.FilledButtonWithIcon

@Composable
fun CrashReportPage(
    errorMessage: String,
    versionReport: String,
    reportUrl: String,
    onExitPressed: () -> Unit
) {
    val uriOpener = LocalUriHandler.current

    val clipboardManager = LocalClipboardManager.current

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            FilledButtonWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .weight(1f),
                onClick = onExitPressed,
                icon = Icons.Rounded.Close,
                text = stringResource(R.string.copy_and_exit)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledButtonWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
                    .weight(1f),
                onClick = {
                    clipboardManager.setText(AnnotatedString(errorMessage))
                    uriOpener.openUri(reportUrl)
                }, icon = Icons.Rounded.CancelScheduleSend, text = stringResource(R.string.report_github)
            )
        }
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = "Bug occurred icon",
                    modifier = Modifier
                        .size(48.dp)
                )
                Text(
                    text = stringResource(R.string.unknown_error_title),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier
                )
                ExpandableElevatedCard(
                    modifier = Modifier,
                    title = stringResource(id = R.string.device_info),
                    subtitle = stringResource(
                        id = R.string.device_info_subtitle
                    ),
                    icon = Icons.Rounded.PermDeviceInformation
                ) {
                    Text(
                        text = versionReport,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            )
        }
    }
}

private const val error_report_fake =
    """java.lang.Exception: Error while initializing Python interpreter: Cannot run program "" : error=2, No such file or directory
	at com.bobbyesp.spotdl_android.SpotDL.init(SpotDL.kt:64)
	at com.bobbyesp.spowlo.App$'$'}2.invokeSuspend(App.kt:43)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
	at kotlinx.coroutines.internal.LimitedDispatcherWorker.run(LimitedDispatcher.kt:115)
	at kotlinx.coroutines.scheduling.TaskImpl.run(Tasks.kt:100)
	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
	at kotlinx.coroutines.scheduling.CoroutineSchedulerWorker.executeTask(CoroutineScheduler.kt:793)
	at kotlinx.coroutines.scheduling.CoroutineSchedulerWorker.runWorker(CoroutineScheduler.kt:697)
	at kotlinx.coroutines.scheduling.CoroutineSchedulerWorker.run(CoroutineScheduler.kt:684)"""

@Preview
@Composable
private fun CrashReportPagePreview() {
    CrashReportPage(
        errorMessage = error_report_fake,
        versionReport = "Version: 1.0.0\nBuild: 1\nDevice: Pixel 4a",
        reportUrl = ""
    ) {}
}