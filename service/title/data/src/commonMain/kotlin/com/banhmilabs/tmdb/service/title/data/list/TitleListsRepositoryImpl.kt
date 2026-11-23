package com.banhmilabs.tmdb.service.title.data.list

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.TitleRefreshPolicy
import com.banhmilabs.tmdb.service.title.data.list.local.TitleListLocalDataSource
import com.banhmilabs.tmdb.service.title.data.list.remote.TitleListRemoteDataSource
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

internal class TitleListsRepositoryImpl(
    private val remote: TitleListRemoteDataSource,
    private val local: TitleListLocalDataSource,
    private val refreshes: KeyedRefreshes<TitleListKey>,
    private val clock: Clock,
) : TitleListsRepository {
    override fun observe(category: TitleCategory, listType: TitleListType, language: Language): Flow<List<Title>?> =
        local.observe(TitleListKey(category = category, listType = listType, language = language.tag))

    override suspend fun refresh(
        category: TitleCategory,
        listType: TitleListType,
        language: Language,
        force: Boolean,
    ): AppResult<Unit> {
        val key = TitleListKey(category = category, listType = listType, language = language.tag)
        return refreshes.joinOrLaunch(
            key = key,
            needed = {
                force || TitleRefreshPolicy.isStale(
                    fetchedAt = local.fetchedAt(key),
                    now = clock.now(),
                    ttl = TitleRefreshPolicy.LIST_TTL,
                )
            },
            refresh = { fetchAndStore(key) },
        )
    }

    private suspend fun fetchAndStore(key: TitleListKey): AppResult<Unit> =
        when (val result = remote.fetch(key = key, now = clock.now())) {
            is AppResult.Failure -> result

            is AppResult.Success -> {
                local.replace(key = key, entries = result.value, fetchedAt = clock.now())
                AppResult.Success(Unit)
            }
        }
}
