package com.bobbyesp.metadator.di

import com.bobbyesp.metadator.presentation.pages.MediaStorePageViewModel
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.MetadataEditorViewModel
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appMainViewModels = module {
    viewModel { MediaStorePageViewModel(get()) }
}

val utilitiesViewModels = module {
    viewModel { MetadataEditorViewModel(get(), get()) }
    viewModel { MetadataBottomSheetViewModel(get()) }
}

val mediaplayerViewModels = module {
    viewModel { MediaplayerViewModel(get(), get(), get(), get()) }
}