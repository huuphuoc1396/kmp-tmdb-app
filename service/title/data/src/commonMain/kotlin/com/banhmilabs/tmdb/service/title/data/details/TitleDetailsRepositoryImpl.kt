package com.banhmilabs.tmdb.service.title.data.details

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.TitleRefreshPolicy
import com.banhmilabs.tmdb.service.title.data.details.local.TitleDetailsLocalDataSource
import com.banhmilabs.tmdb.service.title.data.details.remote.TitleDetailsRemoteDataSource
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleSummary
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

internal class TitleDetailsRepositoryImpl(
    private val remote: TitleDetailsRemoteDataSource,
    private val local: TitleDetailsLocalDataSource,
    private val refreshes: KeyedRefreshes<TitleDetailsKey>,
    private val clock: Clock,
) : TitleDetailsRepository {
    override fun observe(id: TitleId, language: Language): Flow<TitleDetails?> =
        local.observe(TitleDetailsKey(id = id, language = language))

    override fun observeSummary(id: TitleId, language: Language): Flow<TitleSummary?> =
        local.observeSummary(TitleDetailsKey(id = id, language = language))

    override suspend fun refresh(id: TitleId, language: Language, force: Boolean): AppResult<Unit> {
        val key = TitleDetailsKey(id = id, language = language)
        return refreshes.joinOrLaunch(
            key = key,
            needed = {
                force || TitleRefreshPolicy.isStale(
                    fetchedAt = local.fetchedAt(key),
                    now = clock.now(),
                    ttl = TitleRefreshPolicy.DETAILS_TTL,
                )
            },
            refresh = { fetchAndStore(key) },
        )
    }

    private suspend fun fetchAndStore(key: TitleDetailsKey): AppResult<Unit> = when (val result = remote.fetch(key)) {
        is AppResult.Failure -> result

        is AppResult.Success -> {
            local.upsert(key = key, entry = result.value, fetchedAt = clock.now())
            AppResult.Success(Unit)
        }
    }
}
