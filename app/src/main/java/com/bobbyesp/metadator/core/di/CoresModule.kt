package com.bobbyesp.metadator.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bobbyesp.metadator.core.data.local.preferences.AppPreferences
import com.bobbyesp.metadator.core.data.local.preferences.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appCoroutinesScope = module {
    single<CoroutineScope>(
        qualifier = named("AppMainSupervisedScope")
    ) { CoroutineScope(SupervisorJob()) }
}

val coreFunctionalitiesModule = module {
    single<DataStore<Preferences>> {
        androidContext().dataStore
    }
    single<AppPreferences> {
        AppPreferences(
            dataStore = get(),
            scope = get(qualifier = named("AppMainSupervisedScope"))
        )
    }

    single<ImageLoader> {
        val context = androidContext()
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.4)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(7 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(true)
            .crossfade(300)
            .bitmapFactoryMaxParallelism(12)
            .dispatcher(Dispatchers.IO)
            .build()
    }
}