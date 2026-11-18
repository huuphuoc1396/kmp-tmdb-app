package com.banhmilabs.tmdb.core.di

import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
}
