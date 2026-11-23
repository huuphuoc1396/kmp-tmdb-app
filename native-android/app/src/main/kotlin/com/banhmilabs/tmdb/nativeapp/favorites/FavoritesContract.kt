package com.banhmilabs.tmdb.nativeapp.favorites

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle

internal data class FavoritesUiState(
    val favorites: List<FavoriteTitle> = emptyList(),
    val pendingRemoval: FavoriteTitle? = null,
)

internal sealed interface FavoritesIntent {
    data class LanguageChanged(val language: Language) : FavoritesIntent

    data class TitleClicked(val id: TitleId) : FavoritesIntent

    data class RemoveFavoriteClicked(val favorite: FavoriteTitle) : FavoritesIntent

    data object RemoveFavoriteConfirmed : FavoritesIntent

    data object RemoveFavoriteDismissed : FavoritesIntent
}

internal sealed interface FavoritesEffect {
    data class OpenTitle(val id: TitleId) : FavoritesEffect
}
