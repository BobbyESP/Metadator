package com.bobbyesp.metadator.core.domain.settings

import com.bobbyesp.metadator.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun update(transform: (UserSettings) -> UserSettings)
}
