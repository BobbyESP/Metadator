package com.bobbyesp.metadator.mediaplayer.di

import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mediaplayerViewModels = module {
    viewModel { MediaplayerViewModel(get(), get(), get(), get()) }
}