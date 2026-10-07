/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorAccentTheme
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.navigation.Editor
import com.bobbyesp.metadator.core.navigation.Navigator
import com.bobbyesp.metadator.core.navigation.pane.LocalPaneContext
import com.bobbyesp.metadator.core.ui.viewmodel.CollectEffects
import com.bobbyesp.metadator.core.ui.viewmodel.ShowMessages
import com.bobbyesp.metadator.feature.editor.lookup.LookupSheet
import com.bobbyesp.metadator.feature.editor.lookup.LookupViewModel
import com.bobbyesp.metadator.tags.api.TagField
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** The editor's destination: beside the library on a wide window, on its own otherwise. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.editorSection(navigator: Navigator) {
    entry<Editor>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        EditorDestination(uri = key.uri, onClose = { navigator.removeDestination(key) })
    }
}

/** The editor for [uri], for any host: the main activity's stack or the external editor's. */
@Composable
fun EditorDestination(uri: String, onClose: () -> Unit) {
    val viewModel: EditorViewModel = koinViewModel(key = uri) { parametersOf(ContentRef(uri)) }
    val lookup: LookupViewModel = koinViewModel(key = "lookup:$uri")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lookupState by lookup.state.collectAsStateWithLifecycle()
    var showLookup by rememberSaveable(uri) { mutableStateOf(false) }

    ShowMessages(viewModel.messages)
    viewModel.effects.CollectEffects { effect ->
        when (effect) {
            EditorEffect.Close -> onClose()
        }
    }

    val accent =
        state.coverInfo
            ?.dominantColor
            ?.takeIf { state.settings.colorEditorFromArtwork }
            ?.let { Color(it) }

    MetadatorAccentTheme(accent = accent) {
        EditorScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onFindMetadata = {
                val draft = state.draft ?: return@EditorScreen
                showLookup = true
                if (lookupState.title.isEmpty() && lookupState.artist.isEmpty()) {
                    lookup.start(
                        title =
                            draft.tags.first(TagField.Title.key)
                                ?: state.loaded?.track?.title.orEmpty(),
                        artist = draft.tags[TagField.Artist.key].firstOrNull().orEmpty(),
                        album = draft.tags.first(TagField.Album.key).orEmpty(),
                        durationMs =
                            state.loaded?.audio?.durationMs ?: state.loaded?.track?.durationMs,
                    )
                }
            },
            onClose = onClose,
            showClose = LocalPaneContext.current.isSolePane,
        )

        if (showLookup) {
            LookupSheet(
                state = lookupState,
                onQueryChange = { title, artist, album -> lookup.setQuery(title, artist, album) },
                onSearch = lookup::search,
                onOpen = { candidate ->
                    state.draft?.let { lookup.open(candidate, it.tags, state.positionStyle) }
                },
                onToggle = lookup::toggle,
                onToggleCover = lookup::toggleCover,
                onBackToResults = lookup::closeComparison,
                onApply = { fields, coverUrl ->
                    viewModel.onIntent(EditorIntent.ApplyLookup(fields, coverUrl))
                    lookup.closeComparison()
                    showLookup = false
                },
                onDismiss = { showLookup = false },
            )
        }
    }
}
