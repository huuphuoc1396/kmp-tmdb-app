package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [TitleListsRepository] for tests; kept honest by [TitleListsRepositoryContract]. Every refresh stores
 * the list's titles from [setRemoteTitles], else [remoteTitles] (no staleness check).
 */
class FakeTitleListsRepository(private val remoteTitles: List<Title> = emptyList()) : TitleListsRepository {
    private val lists = MutableStateFlow<Map<Key, List<Title>>>(emptyMap())
    private val remoteByList = mutableMapOf<Pair<TitleCategory, TitleListType>, List<Title>>()
    private var nextRefreshError: AppError? = null
    private val _refreshRequests = mutableListOf<RefreshRequest>()

    /** Every [refresh] call so far, failed ones included, in call order. */
    val refreshRequests: List<RefreshRequest> get() = _refreshRequests.toList()

    /** Refreshes of this list, in any language, store [titles] instead of [remoteTitles]. */
    fun setRemoteTitles(category: TitleCategory, listType: TitleListType, titles: List<Title>) {
        remoteByList[category to listType] = titles
    }

    /** The next [refresh] fails with [error] and leaves the cache unchanged. */
    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    override fun observe(category: TitleCategory, listType: TitleListType, language: Language): Flow<List<Title>?> =
        lists.map { it[Key(category, listType, language)] }.distinctUntilChanged()

    override suspend fun refresh(
        category: TitleCategory,
        listType: TitleListType,
        language: Language,
        force: Boolean,
    ): AppResult<Unit> {
        _refreshRequests += RefreshRequest(category, listType, language, force)
        val error = nextRefreshError
        if (error != null) {
            nextRefreshError = null
            return AppResult.Failure(error)
        }
        val titles = remoteByList[category to listType] ?: remoteTitles
        lists.update { it + (Key(category, listType, language) to titles) }
        return AppResult.Success(Unit)
    }

    data class RefreshRequest(
        val category: TitleCategory,
        val listType: TitleListType,
        val language: Language,
        val force: Boolean,
    )

    private data class Key(val category: TitleCategory, val listType: TitleListType, val language: Language)
}
