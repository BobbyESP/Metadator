/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor

import com.bobbyesp.metadator.feature.editor.lookup.LookupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val editorModule = module {
    single { AndroidImageSource(get(), get(), get()) } bind ImageSource::class
    viewModelOf(::EditorViewModel)
    viewModelOf(::LookupViewModel)
}
