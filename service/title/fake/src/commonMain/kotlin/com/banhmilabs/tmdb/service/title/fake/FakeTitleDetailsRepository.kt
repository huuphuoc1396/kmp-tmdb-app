package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [TitleDetailsRepository] for tests; kept honest by [TitleDetailsRepositoryContract]. Every refresh
 * stores the title's details from [remoteDetails] or [setRemoteDetails] (no staleness check); a title with none
 * fails with [AppError.NotFound], as TMDB would.
 */
class FakeTitleDetailsRepository(remoteDetails: Map<TitleId, TitleDetails> = emptyMap()) : TitleDetailsRepository {
    private val cache = MutableStateFlow<Map<Pair<TitleId, Language>, TitleDetails>>(emptyMap())
    private val remote = remoteDetails.toMutableMap()
    private var nextRefreshError: AppError? = null
    private val _refreshRequests = mutableListOf<RefreshRequest>()

    /** Every [refresh] call so far, failed ones included, in call order. */
    val refreshRequests: List<RefreshRequest> get() = _refreshRequests.toList()

    /** Refreshes of [id], in any language, store [details]. */
    fun setRemoteDetails(id: TitleId, details: TitleDetails) {
        remote[id] = details
    }

    /** The next [refresh] fails with [error] and leaves the cache unchanged. */
    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    override fun observe(id: TitleId, language: Language): Flow<TitleDetails?> =
        cache.map { it[id to language] }.distinctUntilChanged()

    override suspend fun refresh(id: TitleId, language: Language, force: Boolean): AppResult<Unit> {
        _refreshRequests += RefreshRequest(id, language, force)
        val error = nextRefreshError.also { nextRefreshError = null }
        val details = remote[id]
        return when {
            error != null -> AppResult.Failure(error)

            details == null -> AppResult.Failure(AppError.NotFound)

            else -> {
                cache.update { it + ((id to language) to details) }
                AppResult.Success(Unit)
            }
        }
    }

    data class RefreshRequest(val id: TitleId, val language: Language, val force: Boolean)
}
