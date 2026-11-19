package com.banhmilabs.tmdb.service.title.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.data.TitleListEntry
import com.banhmilabs.tmdb.service.title.data.TitleListKey
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.time.Instant

private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"

/** The title lists cached in SQLDelight, per language. */
internal class TitleListLocalDataSource(
    private val database: TitleDatabase,
    private val dispatchers: DispatcherProvider,
) {
    private val queries = database.titleListQueries

    /** Cached titles in TMDB order; `null` until the list was stored once, which differs from a list stored empty. */
    fun observe(key: TitleListKey): Flow<List<Title>?> {
        val titles = queries.selectTitles(list = key.list, language = key.language, mapper = ::toTitle)
            .asFlow()
            .mapToList(dispatchers.io)
        val fetchedAt = queries.selectFetchedAt(list = key.list, language = key.language)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
        return combine(titles, fetchedAt) { cached, fetched -> if (fetched == null) null else cached }
            .distinctUntilChanged()
    }

    /** When the list was last stored, or `null` if never. */
    suspend fun fetchedAt(key: TitleListKey): Instant? = withContext(dispatchers.io) {
        queries.selectFetchedAt(list = key.list, language = key.language)
            .executeAsOneOrNull()
            ?.let(Instant::fromEpochMilliseconds)
    }

    /** Replaces the list and its fetch time in one transaction, keeping the order of [entries]. */
    suspend fun replace(key: TitleListKey, entries: List<TitleListEntry>, fetchedAt: Instant): Unit =
        withContext(dispatchers.io) {
            database.transaction {
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
    posterUrl = posterPath?.let { POSTER_BASE_URL + it },
)
