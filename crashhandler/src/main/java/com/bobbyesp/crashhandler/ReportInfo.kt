package com.bobbyesp.crashhandler

data class ReportInfo(
    val androidVersion: Boolean = true,
    val deviceInfo: Boolean = true,
    val supportedABIs: Boolean = true,
)
