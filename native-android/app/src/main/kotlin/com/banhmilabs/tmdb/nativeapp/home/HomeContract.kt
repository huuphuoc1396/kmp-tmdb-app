package com.banhmilabs.tmdb.nativeapp.home

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType

internal sealed interface RowState {
    data object Loading : RowState

    data object Error : RowState

    data class Titles(val titles: List<Title>) : RowState
}

internal data class HomeUiState(
    val selectedCategory: TitleCategory = TitleCategory.MOVIES,
    val featured: RowState = RowState.Loading,
    val popular: RowState = RowState.Loading,
    val isRefreshing: Boolean = false,
    val favorites: List<FavoriteTitle> = emptyList(),
    val favoritesCount: Int = 0,
    val pendingRemoval: FavoriteTitle? = null,
)

internal sealed interface HomeIntent {
    data class LanguageChanged(val language: Language) : HomeIntent

    data class CategorySelected(val category: TitleCategory) : HomeIntent

    data class TitleClicked(val title: Title) : HomeIntent

    data object Refresh : HomeIntent

    data class Retry(val listType: TitleListType) : HomeIntent

    data class FavoriteClicked(val id: TitleId) : HomeIntent

    data object SeeAllFavoritesClicked : HomeIntent

    data class RemoveFavoriteClicked(val favorite: FavoriteTitle) : HomeIntent

    data object RemoveFavoriteConfirmed : HomeIntent

    data object RemoveFavoriteDismissed : HomeIntent
}

internal sealed interface HomeEffect {
    data class OpenTitle(val title: Title) : HomeEffect

    data class OpenFavorite(val id: TitleId) : HomeEffect

    data object OpenFavorites : HomeEffect
}
