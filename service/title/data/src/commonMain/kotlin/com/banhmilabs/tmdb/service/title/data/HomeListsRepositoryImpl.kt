package com.banhmilabs.tmdb.service.title.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.network.apiCall
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.HomeCategory
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import com.banhmilabs.tmdb.service.title.domain.HomeRow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"

/** [HomeListsRepository] reading the SQLDelight cache and refreshing it from TMDB in [scope]. */
internal class HomeListsRepositoryImpl(
    private val database: TitleDatabase,
    private val httpClient: HttpClient,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
) : HomeListsRepository {
    private val queries get() = database.homeListQueries
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<Key, Deferred<AppResult<Unit>>>()

    override fun observe(category: HomeCategory, row: HomeRow, language: Language): Flow<List<Title>?> {
        val key = Key(category, row, language)
        val titles = queries.selectTitles(key.list, key.tag) { tmdbId, type, name, posterPath ->
            Title(TitleId(tmdbId, TitleType.valueOf(type)), name, posterPath?.let { POSTER_BASE_URL + it })
        }.asFlow().mapToList(dispatchers.io)
        val fetchedAt = queries.selectFetchedAt(key.list, key.tag).asFlow().mapToOneOrNull(dispatchers.io)
        return combine(titles, fetchedAt) { list, fetched -> list.takeIf { fetched != null } }.distinctUntilChanged()
    }

    override suspend fun refresh(
        category: HomeCategory,
        row: HomeRow,
        language: Language,
        force: Boolean,
    ): AppResult<Unit> {
        val key = Key(category, row, language)
        val refresh = mutex.withLock {
            inFlight[key] ?: run {
                if (!force && !TitleRefreshPolicy.isStale(fetchedAtMillis(key), clock.now())) {
                    return AppResult.Success(Unit)
                }
                scope.async {
                    try {
                        fetchAndStore(key)
                    } finally {
                        mutex.withLock { inFlight.remove(key) }
                    }
                }.also { inFlight[key] = it }
            }
        }
        return refresh.await()
    }

    private suspend fun fetchedAtMillis(key: Key): Long? = withContext(dispatchers.io) {
        queries.selectFetchedAt(key.list, key.tag).executeAsOneOrNull()
    }

    private suspend fun fetchAndStore(key: Key): AppResult<Unit> {
        val request = homeListRequest(key.category, key.row, clock.now())
        val result = apiCall {
            httpClient.get(request.path) {
                parameter("language", key.tag)
                parameter("page", 1)
                request.parameters.forEach { (name, value) -> parameter(name, value) }
            }.body<TmdbPageDto>()
        }
        return when (result) {
            is AppResult.Failure -> result

            is AppResult.Success -> {
                store(key, request.type, result.value.results)
                AppResult.Success(Unit)
            }
        }
    }

    private suspend fun store(key: Key, type: TitleType, results: List<TmdbTitleDto>) = withContext(dispatchers.io) {
        val fetchedAt = clock.now().toEpochMilliseconds()
        database.transaction {
            queries.deleteTitles(key.list, key.tag)
            results.mapNotNull { dto -> (dto.title ?: dto.name)?.let { name -> dto to name } }
                .forEachIndexed { position, (dto, name) ->
                    queries.insertTitle(
                        key.list,
                        key.tag,
                        position.toLong(),
                        dto.id,
                        type.name,
                        name,
                        dto.posterPath,
                    )
                }
            queries.upsertFetchedAt(key.list, key.tag, fetchedAt)
        }
    }

    private data class Key(val category: HomeCategory, val row: HomeRow, val language: Language) {
        val list = "${category.name}_${row.name}"
        val tag = language.tag
    }
}
