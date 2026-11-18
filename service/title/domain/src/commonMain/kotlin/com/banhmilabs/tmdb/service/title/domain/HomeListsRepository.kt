package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import kotlinx.coroutines.flow.Flow

/** The Home lists, cached per language; reads never wait for the network. */
interface HomeListsRepository {
    /** Cached titles in TMDB order; `null` until a refresh of this list and language has succeeded. */
    fun observe(category: HomeCategory, row: HomeRow, language: Language): Flow<List<Title>?>

    /** Fetches the list when stale (older than 6 h) or [force]d; shares a running refresh. Failures keep the cache. */
    suspend fun refresh(
        category: HomeCategory,
        row: HomeRow,
        language: Language,
        force: Boolean = false,
    ): AppResult<Unit>
}
