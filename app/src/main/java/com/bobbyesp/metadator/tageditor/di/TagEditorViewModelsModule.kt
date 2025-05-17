package com.bobbyesp.metadator.tageditor.di

import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.MetadataEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tagEditorViewModelsModule = module {
    viewModel { MetadataEditorViewModel(get(), get(), get()) }
}
