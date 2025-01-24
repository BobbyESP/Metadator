package com.bobbyesp.metadator.di

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appSystemManagers = module {
    single<ClipboardManager> { androidApplication().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager }
    single<ConnectivityManager> { androidContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }
}