package com.bobbyesp.metadator.di

import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.MetadataEditorVM
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.MetadataBsVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appMainViewModels = module {
    viewModel { MediaStorePageViewModel(get()) }
}

val utilitiesViewModels = module {
    viewModel { MetadataEditorVM(get(), get()) }
    viewModel { MetadataBsVM(get()) }
}

val mediaplayerViewModels = module {
    viewModel { MediaplayerViewModel(get(), get(), get(), get()) }
}