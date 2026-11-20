package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.coroutines.flow.Flow

/** Details of a title, cached per language; reads never wait for the network. */
interface TitleDetailsRepository {
    /** Cached details; `null` until a refresh of this title and language has succeeded. */
    fun observe(id: TitleId, language: Language): Flow<TitleDetails?>

    /** Fetches when stale (older than 24 h) or [force]d; shares a running refresh. Failures keep the cache. */
    suspend fun refresh(id: TitleId, language: Language, force: Boolean = false): AppResult<Unit>
}
