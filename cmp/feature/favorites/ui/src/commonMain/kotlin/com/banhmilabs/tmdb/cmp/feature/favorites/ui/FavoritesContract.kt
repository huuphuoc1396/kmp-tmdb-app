package com.banhmilabs.tmdb.cmp.feature.favorites.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle

internal data class FavoritesUiState(val favorites: List<FavoriteTitle> = emptyList())

internal sealed interface FavoritesIntent {
    data class LanguageChanged(val language: Language) : FavoritesIntent

    data class TitleClicked(val id: TitleId) : FavoritesIntent
}

internal sealed interface FavoritesEffect {
    data class OpenTitle(val id: TitleId) : FavoritesEffect
}
