package com.bobbyesp.metadator.tageditor.di

import com.bobbyesp.metadator.tageditor.data.local.UriToPictureConverter
import com.bobbyesp.metadator.tageditor.data.local.repository.AudioMetadataRepositoryImpl
import com.bobbyesp.metadator.tageditor.model.repository.AudioMetadataRepository
import com.bobbyesp.utilities.mediastore.data.local.repository.MediaStoreUseCaseImpl
import com.bobbyesp.utilities.mediastore.model.repository.MediaStoreUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val tagEditorModule = module {
    single<MediaStoreUseCase> { MediaStoreUseCaseImpl(context = androidContext()) }

    single<UriToPictureConverter> { UriToPictureConverter(androidContext()) }

    single<AudioMetadataRepository> { AudioMetadataRepositoryImpl(get()) }
}
