package com.bobbyesp.crashhandler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.File

class CrashHandlerViewModel : ViewModel() {
    private val _log: MutableStateFlow<String> = MutableStateFlow("")
    val log: StateFlow<String> = _log.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    fun loadLog(logfilePath: String) {
        val logFile = File(logfilePath)
        val log = logFile.bufferedReader().use { it.readText() }

        val transformedText: String = log.let { text ->
            val stringBuilder = StringBuilder()
            if (text.length > 2000) {
                stringBuilder
                    .append(text.substring(0, 2000))
                    .append("...")
                    .toString()
            } else text
        }

        _log.update { transformedText }
    }

    fun generateReportToSend(versionReport: String): String {
        val sb: StringBuilder = StringBuilder()

        sb.append(versionReport).append("\n").append(_log.value)

        return sb.toString()
    }
}