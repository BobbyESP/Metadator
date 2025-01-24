package com.bobbyesp.metadator.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bobbyesp.metadator.util.preferences.AppPreferences
import com.bobbyesp.metadator.util.preferences.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
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
    single<DataStore<Preferences>> { androidContext().dataStore }
    single<AppPreferences> {
        AppPreferences(
            dataStore = androidContext().dataStore,
            scope = get(qualifier = named("AppMainSupervisedScope"))
        )
    }
}