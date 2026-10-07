package com.bobbyesp.metadator.feature.settings

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module { viewModelOf(::SettingsViewModel) }
