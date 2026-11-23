package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.coroutines.flow.Flow

interface TitleDetailsRepository {
    fun observe(id: TitleId, language: Language): Flow<TitleDetails?>

    fun observeSummary(id: TitleId, language: Language): Flow<TitleSummary?>

    suspend fun refresh(id: TitleId, language: Language, force: Boolean = false): AppResult<Unit>
}
