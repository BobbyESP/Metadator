package com.bobbyesp.metadator.features.spotify.di

import com.bobbyesp.metadator.features.spotify.data.repository.SearchRepositoryImpl
import com.bobbyesp.metadator.features.spotify.domain.repositories.SearchRepository
import com.bobbyesp.metadator.features.spotify.domain.services.search.SpotifySearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyRepositoriesModule {
    @Provides
    @Singleton
    fun provideSearchRepository(
        spotifySearchService: SpotifySearchService,
    ): SearchRepository {
        return SearchRepositoryImpl(spotifySearchService)
    }
}