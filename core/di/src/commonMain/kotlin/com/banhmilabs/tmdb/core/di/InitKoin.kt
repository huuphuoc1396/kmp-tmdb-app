package com.banhmilabs.tmdb.core.di

import com.banhmilabs.tmdb.core.common.DefaultDispatcherProvider
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.service.settings.data.di.settingsDataModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

/**
 * Starts Koin with every shared-core module. Call it once per process from the app shell; platforms add their own
 * declarations through [appDeclaration] (Android: `androidContext(...)`).
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(coreModule, settingsDataModule)
}
