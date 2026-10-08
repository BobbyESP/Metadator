/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.common.AppInfo
import com.bobbyesp.metadator.core.domain.lookup.LookupService
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.UserSettings
import com.bobbyesp.metadator.tags.api.TagBackupStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProviderInfo(val id: String, val name: String)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backups: TagBackupStore,
    lookup: LookupService,
    val appInfo: AppInfo,
) : ViewModel() {
    val settings: StateFlow<UserSettings> =
        repository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserSettings(),
        )

    val providers: List<ProviderInfo> =
        lookup.availableProviders.map { ProviderInfo(it.id, it.displayName) }

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun toggleProvider(id: String) = update { settings ->
        settings.copy(
            enabledProviders =
                if (id in settings.enabledProviders) settings.enabledProviders - id
                else settings.enabledProviders + id
        )
    }

    fun clearHistory(onDone: () -> Unit) {
        viewModelScope.launch {
            backups.clear()
            onDone()
        }
    }
}
