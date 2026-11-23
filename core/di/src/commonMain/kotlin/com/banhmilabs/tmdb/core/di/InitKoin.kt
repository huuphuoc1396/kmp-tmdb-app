package com.banhmilabs.tmdb.core.di

import com.banhmilabs.tmdb.core.common.DefaultDispatcherProvider
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.network.createTmdbHttpClient
import com.banhmilabs.tmdb.service.favorites.data.di.favoritesDataModule
import com.banhmilabs.tmdb.service.settings.data.di.settingsDataModule
import com.banhmilabs.tmdb.service.title.data.di.titleDataModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

private val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { Clock.System }
    single { createTmdbHttpClient() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(coreModule, platformModule, settingsDataModule, titleDataModule, favoritesDataModule)
}
