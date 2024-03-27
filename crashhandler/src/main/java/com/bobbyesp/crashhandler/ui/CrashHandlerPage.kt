package com.bobbyesp.crashhandler.ui

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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.PermDeviceInformation
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
import java.net.URLEncoder

@Composable
private fun CrashReportPage(
    versionReport: String = "VERSION REPORT",
    errorMessage: String = error_report_fake,
    onClick: () -> Unit = {}
) {
    val uriOpener = LocalUriHandler.current

    val clipboardManager = LocalClipboardManager.current

    //cut the error message to 1000 characters
    val errorMessageCut = if (errorMessage.length > 2000) {
        errorMessage.substring(0, 2000) + "..."
    } else {
        errorMessage
    }

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp)
        ) {
            FilledButtonWithIcon(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp).weight(1f),
                onClick = onClick,
                icon = Icons.Outlined.BugReport,
                text = stringResource(R.string.copy_and_exit)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledButtonWithIcon(
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp).weight(1f), onClick = {
                    val title = URLEncoder.encode("[App crash]", "UTF-8")
                    clipboardManager.setText(AnnotatedString(errorMessageCut))
                    uriOpener.openUri("https://github.com/BobbyESP/Metadator/issues/new?assignees=&labels=bug&projects=&template=bug_report.yml&title=$title")
                }, icon = Icons.Outlined.BugReport, text = stringResource(R.string.report_github)
            )
        }
    }) {
        Column(
            modifier = Modifier.padding(it).verticalScroll(rememberScrollState()).fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = "Bug occurred icon",
                modifier = Modifier.padding(start = 16.dp).padding(top = 16.dp).size(48.dp)
            )
            Text(
                text = stringResource(R.string.unknown_error_title),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp).padding(horizontal = 16.dp)
            )
            ExpandableElevatedCard(
                modifier = Modifier.padding(16.dp),
                title = stringResource(id = R.string.device_info),
                subtitle = stringResource(
                    id = R.string.device_info_subtitle
                ),
                icon = Icons.Outlined.PermDeviceInformation
            ) {
                Text(
                    text = versionReport,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
private fun CrashHandlerPagePreview() {
    CrashReportPage()
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