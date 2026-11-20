package com.banhmilabs.tmdb.service.title.data.di

import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.TitleDetailsRepositoryImpl
import com.banhmilabs.tmdb.service.title.data.TitleListsRepositoryImpl
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.data.local.TitleDetailsLocalDataSource
import com.banhmilabs.tmdb.service.title.data.local.TitleListLocalDataSource
import com.banhmilabs.tmdb.service.title.data.remote.TitleDetailsRemoteDataSource
import com.banhmilabs.tmdb.service.title.data.remote.TitleListRemoteDataSource
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal const val TITLE_DATABASE_NAME = "title.db"

/** Koin bindings of the title context. */
val titleDataModule = module {
    single { TitleDatabase(get<DatabaseDriverFactory>().create(TitleDatabase.Schema, TITLE_DATABASE_NAME)) }
    single { TitleListRemoteDataSource(httpClient = get()) }
    single { TitleListLocalDataSource(database = get(), dispatchers = get()) }
    single<TitleListsRepository> {
        TitleListsRepositoryImpl(
            remote = get(),
            local = get(),
            refreshes = KeyedRefreshes(refreshScope()),
            clock = get(),
        )
    }
    single { TitleDetailsRemoteDataSource(httpClient = get()) }
    single { TitleDetailsLocalDataSource(database = get(), dispatchers = get()) }
    single<TitleDetailsRepository> {
        TitleDetailsRepositoryImpl(
            remote = get(),
            local = get(),
            refreshes = KeyedRefreshes(refreshScope()),
            clock = get(),
        )
    }
}

/** Where a repository's refreshes run: off the main thread, outliving their callers. */
private fun Scope.refreshScope() = CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io)
