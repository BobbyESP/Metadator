package com.bobbyesp.metadator.tageditor.di

import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.MetadataEditorViewModel
import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tagEditorViewModelsModule = module {
  viewModel { MetadataEditorViewModel(context = androidContext(), stateHandle = get()) }
  viewModel { MetadataBottomSheetViewModel(searchService = get()) }
}
