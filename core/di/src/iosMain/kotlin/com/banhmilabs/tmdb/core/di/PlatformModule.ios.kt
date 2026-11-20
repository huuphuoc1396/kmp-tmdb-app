package com.banhmilabs.tmdb.core.di

import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import com.banhmilabs.tmdb.core.security.DatabaseKeys
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single { DatabaseKeys() }
}
