package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.HomeCategory
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import com.banhmilabs.tmdb.service.title.domain.HomeRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [HomeListsRepository] for tests; kept honest by [HomeListsRepositoryContract]. Every refresh stores
 * [remoteTitles] (no staleness check).
 */
class FakeHomeListsRepository(private val remoteTitles: List<Title> = emptyList()) : HomeListsRepository {
    private val lists = MutableStateFlow<Map<Key, List<Title>>>(emptyMap())
    private var nextRefreshError: AppError? = null

    /** The next [refresh] fails with [error] and leaves the cache unchanged. */
    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    override fun observe(category: HomeCategory, row: HomeRow, language: Language): Flow<List<Title>?> =
        lists.map { it[Key(category, row, language)] }.distinctUntilChanged()

    override suspend fun refresh(
        category: HomeCategory,
        row: HomeRow,
        language: Language,
        force: Boolean,
    ): AppResult<Unit> {
        val error = nextRefreshError
        if (error != null) {
            nextRefreshError = null
            return AppResult.Failure(error)
        }
        lists.update { it + (Key(category, row, language) to remoteTitles) }
        return AppResult.Success(Unit)
    }

    private data class Key(val category: HomeCategory, val row: HomeRow, val language: Language)
}
