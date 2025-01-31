package com.bobbyesp.metadator.mediastore.di

import com.bobbyesp.metadator.mediastore.presentation.MediaStorePageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mediaStoreViewModelsModule = module {
    viewModel { MediaStorePageViewModel(get()) }
}