package com.banhmilabs.tmdb.service.favorites.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.favorites.data.generated.FavoritesDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal class FavoritesLocalDataSource(
    private val database: Lazy<FavoritesDatabase>,
    private val dispatchers: DispatcherProvider,
) {
    private val queries by lazy { database.value.favoriteQueries }

    fun observeAll(): Flow<List<TitleId>> = flow {
        emitAll(
            queries.selectAllNewestFirst { tmdbId, type -> TitleId(tmdbId = tmdbId, type = TitleType.valueOf(type)) }
                .asFlow()
                .mapToList(dispatchers.io),
        )
    }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    fun observe(id: TitleId): Flow<Boolean> = flow {
        emitAll(queries.isFavorite(tmdbId = id.tmdbId, type = id.type.name).asFlow().mapToOne(dispatchers.io))
    }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    suspend fun insert(id: TitleId, addedAt: Instant): Unit = withContext(dispatchers.io) {
        queries.insert(tmdbId = id.tmdbId, type = id.type.name, addedAt = addedAt.toEpochMilliseconds())
    }

    suspend fun delete(id: TitleId): Unit = withContext(dispatchers.io) {
        queries.delete(tmdbId = id.tmdbId, type = id.type.name)
    }
}
