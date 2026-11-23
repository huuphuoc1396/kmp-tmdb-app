package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import kotlinx.coroutines.flow.Flow

interface TitleListsRepository {
    fun observe(category: TitleCategory, listType: TitleListType, language: Language): Flow<List<Title>?>

    suspend fun refresh(
        category: TitleCategory,
        listType: TitleListType,
        language: Language,
        force: Boolean = false,
    ): AppResult<Unit>
}
