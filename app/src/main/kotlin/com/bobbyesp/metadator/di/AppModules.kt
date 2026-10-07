/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.di

import com.bobbyesp.metadator.BuildConfig
import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.common.AppInfo
import com.bobbyesp.metadator.core.common.StringProvider
import com.bobbyesp.metadator.core.data.AndroidStringProvider
import com.bobbyesp.metadator.core.data.DataStoreSettingsRepository
import com.bobbyesp.metadator.core.data.createSettingsDataStore
import com.bobbyesp.metadator.core.database.MetadatorDatabase
import com.bobbyesp.metadator.core.database.RoomTagBackupStore
import com.bobbyesp.metadator.core.database.TagBackupFiles
import com.bobbyesp.metadator.core.domain.batch.BatchRunner
import com.bobbyesp.metadator.core.domain.editor.LoadTrackUseCase
import com.bobbyesp.metadator.core.domain.lookup.LookupService
import com.bobbyesp.metadator.core.domain.lyrics.FindLyricsUseCase
import com.bobbyesp.metadator.core.domain.lyrics.LoadLyricsUseCase
import com.bobbyesp.metadator.core.domain.save.RestoreBackupUseCase
import com.bobbyesp.metadator.core.domain.save.SaveTagChangesUseCase
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.network.HttpClientFactory
import com.bobbyesp.metadator.core.network.KtorArtworkDownloader
import com.bobbyesp.metadator.feature.batch.batchModule
import com.bobbyesp.metadator.feature.editor.editorModule
import com.bobbyesp.metadator.feature.library.libraryModule
import com.bobbyesp.metadator.feature.settings.settingsModule
import com.bobbyesp.metadator.library.api.AudioFileInfo
import com.bobbyesp.metadator.library.api.AudioFileOpener
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.library.api.MediaIndexer
import com.bobbyesp.metadator.library.api.WriteAccess
import com.bobbyesp.metadator.library.mediastore.ActivityResultHost
import com.bobbyesp.metadator.library.mediastore.ContentResolverFileInfo
import com.bobbyesp.metadator.library.mediastore.ContentResolverFileOpener
import com.bobbyesp.metadator.library.mediastore.MediaStoreAudioLibrary
import com.bobbyesp.metadator.library.mediastore.MediaStoreIndexer
import com.bobbyesp.metadator.library.mediastore.MediaStoreWriteAccess
import com.bobbyesp.metadator.library.mediastore.RecoverableAccessCache
import com.bobbyesp.metadator.lookup.api.ArtworkDownloader
import com.bobbyesp.metadator.lookup.deezer.DeezerProvider
import com.bobbyesp.metadator.lookup.musicbrainz.MusicBrainzProvider
import com.bobbyesp.metadator.lyrics.api.LyricsProvider
import com.bobbyesp.metadator.lyrics.lrclib.LrcLibProvider
import com.bobbyesp.metadator.player.api.PlayerController
import com.bobbyesp.metadator.player.media3.Media3PlayerController
import com.bobbyesp.metadator.tags.api.TagBackupStore
import com.bobbyesp.metadator.tags.api.TagReader
import com.bobbyesp.metadator.tags.api.TagWriter
import com.bobbyesp.metadator.tags.taglib.TagLibTagFiles
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

/** Work that must outlive a screen (a batch edit) runs here. */
val AppScope = named("app")

/** The player is driven from the main thread, as Media3 requires. */
val MainScope = named("main")

private val appModule = module {
    single { AppDispatchers() }
    single<CoroutineScope>(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<CoroutineScope>(MainScope) {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
    single {
        AppInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            isPlayStoreBuild = BuildConfig.FLAVOR == "playstore",
        )
    }
    single<StringProvider> { AndroidStringProvider(androidContext()) }
    single<SettingsRepository> {
        DataStoreSettingsRepository(createSettingsDataStore(androidContext()))
    }
}

/*
 * Engines. Each contract is bound to its implementation here and nowhere else: swapping TagLib, the
 * library or a metadata source is a change to one line of this file.
 */
private val engineModule = module {
    single { MetadatorDatabase.build(androidContext()) }
    single<TagBackupStore> {
        RoomTagBackupStore(
            dao = get<MetadatorDatabase>().tagBackups(),
            files = TagBackupFiles(File(androidContext().filesDir, "backups")),
            dispatchers = get(),
        )
    }

    single { RecoverableAccessCache() }
    single { ActivityResultHost() }
    single<AudioFileOpener> { ContentResolverFileOpener(androidContext(), get()) }
    single<AudioLibrary> { MediaStoreAudioLibrary(androidContext(), get()) }
    single<AudioFileInfo> { ContentResolverFileInfo(androidContext(), get()) }
    single<WriteAccess> { MediaStoreWriteAccess(androidContext(), get(), get()) }
    single<MediaIndexer> { MediaStoreIndexer(androidContext(), get()) }
    single { TagLibTagFiles(get(), get()) } binds arrayOf(TagReader::class, TagWriter::class)

    single { HttpClientFactory.create(HttpClientFactory.userAgent(BuildConfig.VERSION_NAME)) }
    single<ArtworkDownloader> { KtorArtworkDownloader(get()) }
    single { MusicBrainzProvider(get()) }
    single { DeezerProvider(get()) }
    single<LyricsProvider> { LrcLibProvider(get()) }

    single<PlayerController> { Media3PlayerController(androidContext(), get(MainScope)) }
}

private val domainModule = module {
    factory { LoadTrackUseCase(get(), get(), get()) }
    factory { SaveTagChangesUseCase(get(), get(), get(), get()) }
    factory { RestoreBackupUseCase(get(), get(), get()) }
    factory { FindLyricsUseCase(get()) }
    factory { LoadLyricsUseCase(get(), get()) }
    single { LookupService(listOf(get<MusicBrainzProvider>(), get<DeezerProvider>()), get()) }
    single { BatchRunner(get(AppScope), get()) }
}

val appModules =
    listOf(
        appModule,
        engineModule,
        domainModule,
        libraryModule,
        editorModule,
        batchModule,
        settingsModule,
    )
