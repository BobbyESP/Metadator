/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Injected rather than referenced, so tests can run everything on one test dispatcher. */
data class AppDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
)

/** What the app knows about its own build, for the About page. */
data class AppInfo(val versionName: String, val versionCode: Long, val isPlayStoreBuild: Boolean)
