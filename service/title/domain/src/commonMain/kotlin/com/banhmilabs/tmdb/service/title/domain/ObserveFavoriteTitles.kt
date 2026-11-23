package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ObserveFavoriteTitles(private val favorites: FavoritesRepository, private val details: TitleDetailsRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(language: Language): Flow<List<FavoriteTitle>> =
        favorites.observeFavorites().flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(ids.map { observeOne(it, language) }) { it.toList() }
            }
        }

    private fun observeOne(id: TitleId, language: Language): Flow<FavoriteTitle> = channelFlow {
        val fallbackLanguage = Language.entries.first { it != language }
        launch {
            if (details.observeSummary(id, language).first() == null) details.refresh(id, language)
        }
        combine(
            details.observeSummary(id, language),
            details.observeSummary(id, fallbackLanguage),
        ) { current, fallback -> FavoriteTitle(id, current ?: fallback) }
            .collect { send(it) }
    }.distinctUntilChanged()
}
