package com.bobbyesp.metadator.feature.batch

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val batchModule = module {
    single { AndroidCoverReader(get(), get()) } bind CoverReader::class
    viewModelOf(::BatchViewModel)
}
