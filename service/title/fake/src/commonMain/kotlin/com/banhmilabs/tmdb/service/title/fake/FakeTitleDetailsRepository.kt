package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTitleDetailsRepository(remoteDetails: Map<TitleId, TitleDetails> = emptyMap()) : TitleDetailsRepository {
    private val cache = MutableStateFlow<Map<Pair<TitleId, Language>, TitleDetails>>(emptyMap())
    private val remote = remoteDetails.toMutableMap()
    private var nextRefreshError: AppError? = null
    private val _refreshRequests = mutableListOf<RefreshRequest>()

    val refreshRequests: List<RefreshRequest> get() = _refreshRequests.toList()

    fun setRemoteDetails(id: TitleId, details: TitleDetails) {
        remote[id] = details
    }

    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    override fun observe(id: TitleId, language: Language): Flow<TitleDetails?> =
        cache.map { it[id to language] }.distinctUntilChanged()

    override fun observeSummary(id: TitleId, language: Language): Flow<TitleSummary?> =
        observe(id, language).map { details ->
            details?.let {
                TitleSummary(
                    id = it.id,
                    name = it.name,
                    posterUrl = it.artworkUrl,
                    releaseYear = it.releaseYear,
                    firstGenre = it.genres.firstOrNull(),
                )
            }
        }

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
