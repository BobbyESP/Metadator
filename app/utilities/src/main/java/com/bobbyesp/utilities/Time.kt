package com.bobbyesp.utilities

import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object Time {
    @OptIn(ExperimentalTime::class)
    fun getZuluTimeSnapshot(): String {
        val instant = Clock.System.now()
        return instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    }

    fun formatDuration(duration: Long): String {
        val minutes: Long = duration / 60000
        val seconds: Long = (duration % 60000) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
