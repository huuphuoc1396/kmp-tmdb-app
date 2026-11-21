package com.banhmilabs.tmdb.service.favorites.data.di

import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import com.banhmilabs.tmdb.core.security.DatabaseKeys
import com.banhmilabs.tmdb.service.favorites.data.FavoritesRepositoryImpl
import com.banhmilabs.tmdb.service.favorites.data.generated.FavoritesDatabase
import com.banhmilabs.tmdb.service.favorites.data.local.FavoritesLocalDataSource
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import org.koin.dsl.module

internal const val FAVORITES_DATABASE_NAME = "favorites.db"

/** Koin bindings of the favorites context. */
val favoritesDataModule = module {
    single {
        val driver = get<DatabaseDriverFactory>().create(
            schema = FavoritesDatabase.Schema,
            name = FAVORITES_DATABASE_NAME,
            passphrase = get<DatabaseKeys>().passphrase(FAVORITES_DATABASE_NAME),
        )
        FavoritesDatabase(driver)
    }
    single { FavoritesLocalDataSource(database = lazy { get<FavoritesDatabase>() }, dispatchers = get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(local = get(), clock = get()) }
}
