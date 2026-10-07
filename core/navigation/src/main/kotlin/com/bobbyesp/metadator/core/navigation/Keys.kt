/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation

import androidx.navigation3.runtime.NavKey
import com.bobbyesp.metadator.core.model.CollectionType
import kotlinx.serialization.Serializable

/*
 * Every destination of the app. They live together because features may not depend on each
 * other: a feature reaches another only through these keys. Keys carry ids, never models, so a
 * back stack restored after process death reloads fresh data.
 */

/** The library: songs, albums, artists and folders. The root of the stack. */
@Serializable data object Library : NavKey

/** The songs of an album, an artist or a folder. */
@Serializable
data class Collection(val type: CollectionType, val key: String, val title: String) : NavKey

/** The tag editor for one file, by its `content://` URI. */
@Serializable data class Editor(val uri: String) : NavKey

/** Editing several files at once. */
@Serializable data class BatchEditor(val uris: List<String>) : NavKey

@Serializable data object Settings : NavKey

@Serializable data object AppearanceSettings : NavKey

@Serializable data object EditorSettings : NavKey

@Serializable data object LookupSettings : NavKey

@Serializable data object About : NavKey
