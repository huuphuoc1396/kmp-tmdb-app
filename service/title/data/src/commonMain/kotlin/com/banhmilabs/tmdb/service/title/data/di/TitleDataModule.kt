package com.banhmilabs.tmdb.service.title.data.di

import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import com.banhmilabs.tmdb.service.title.data.HomeListsRepositoryImpl
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

internal const val TITLE_DATABASE_NAME = "title.db"

/** Koin bindings of the title context. */
val titleDataModule = module {
    single { TitleDatabase(get<DatabaseDriverFactory>().create(TitleDatabase.Schema, TITLE_DATABASE_NAME)) }
    single<HomeListsRepository> {
        HomeListsRepositoryImpl(
            database = get(),
            httpClient = get(),
            clock = get(),
            dispatchers = get(),
            scope = CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io),
        )
    }
}
