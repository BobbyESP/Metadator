/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.batch

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bobbyesp.metadator.core.navigation.BatchEditor
import com.bobbyesp.metadator.core.navigation.Navigator
import com.bobbyesp.metadator.core.ui.viewmodel.CollectEffects
import com.bobbyesp.metadator.core.ui.viewmodel.ShowMessages
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.batchSection(navigator: Navigator) {
    entry<BatchEditor> { key ->
        val viewModel: BatchViewModel =
            koinViewModel(key = key.uris.hashCode().toString()) { parametersOf(key.uris) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        ShowMessages(viewModel.messages)
        viewModel.effects.CollectEffects { effect ->
            when (effect) {
                BatchEffect.Close -> navigator.removeDestination(key)
            }
        }
        BatchScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onClose = { navigator.removeDestination(key) },
        )
    }
}
