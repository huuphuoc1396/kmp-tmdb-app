package com.banhmilabs.tmdb.service.title.data.list.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.data.TitleEntry
import com.banhmilabs.tmdb.service.title.data.TmdbImages
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.data.list.TitleListKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal class TitleListLocalDataSource(
    private val database: Lazy<TitleDatabase>,
    private val dispatchers: DispatcherProvider,
) {
    private val queries by lazy { database.value.titleListQueries }

    fun observe(key: TitleListKey): Flow<List<Title>?> = flow { emitAll(observeCached(key)) }
        .flowOn(dispatchers.io)

    private fun observeCached(key: TitleListKey): Flow<List<Title>?> {
        val titles = queries.selectTitles(list = key.list, language = key.language, mapper = ::toTitle)
            .asFlow()
            .mapToList(dispatchers.io)
        val fetchedAt = queries.selectFetchedAt(list = key.list, language = key.language)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
        return combine(titles, fetchedAt) { cached, fetched -> if (fetched == null) null else cached }
            .distinctUntilChanged()
    }

    suspend fun fetchedAt(key: TitleListKey): Instant? = withContext(dispatchers.io) {
        queries.selectFetchedAt(list = key.list, language = key.language)
            .executeAsOneOrNull()
            ?.let(Instant::fromEpochMilliseconds)
    }

    suspend fun replace(key: TitleListKey, entries: List<TitleEntry>, fetchedAt: Instant): Unit =
        withContext(dispatchers.io) {
            database.value.transaction {
                queries.deleteTitles(list = key.list, language = key.language)
                entries.forEachIndexed { position, entry ->
                    queries.insertTitle(
                        list = key.list,
                        language = key.language,
                        position = position.toLong(),
                        tmdbId = entry.tmdbId,
                        type = entry.type.name,
                        name = entry.name,
                        posterPath = entry.posterPath,
                    )
                }
                queries.upsertFetchedAt(
                    list = key.list,
                    language = key.language,
                    fetchedAt = fetchedAt.toEpochMilliseconds(),
                )
            }
        }
}

private fun toTitle(tmdbId: Long, type: String, name: String, posterPath: String?) = Title(
    id = TitleId(tmdbId = tmdbId, type = TitleType.valueOf(type)),
    name = name,
    posterUrl = posterPath?.let(TmdbImages::poster),
)
