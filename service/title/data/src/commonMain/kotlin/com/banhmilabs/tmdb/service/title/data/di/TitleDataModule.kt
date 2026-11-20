package com.banhmilabs.tmdb.service.title.data.di

import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.database.DatabaseDriverFactory
import com.banhmilabs.tmdb.core.security.DatabaseKeys
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsRepositoryImpl
import com.banhmilabs.tmdb.service.title.data.details.local.TitleDetailsLocalDataSource
import com.banhmilabs.tmdb.service.title.data.details.remote.TitleDetailsRemoteDataSource
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.data.list.TitleListsRepositoryImpl
import com.banhmilabs.tmdb.service.title.data.list.local.TitleListLocalDataSource
import com.banhmilabs.tmdb.service.title.data.list.remote.TitleListRemoteDataSource
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal const val TITLE_DATABASE_NAME = "title.db"

/** Koin bindings of the title context. */
val titleDataModule = module {
    single {
        val driver = get<DatabaseDriverFactory>().create(
            schema = TitleDatabase.Schema,
            name = TITLE_DATABASE_NAME,
            passphrase = get<DatabaseKeys>().passphrase(TITLE_DATABASE_NAME),
        )
        TitleDatabase(driver)
    }
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
