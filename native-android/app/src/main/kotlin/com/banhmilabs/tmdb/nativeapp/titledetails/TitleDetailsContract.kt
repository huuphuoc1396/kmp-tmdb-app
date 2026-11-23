package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleDetails

internal sealed interface DetailsLoad {
    data object Loading : DetailsLoad

    data object Error : DetailsLoad

    data class Loaded(val details: TitleDetails) : DetailsLoad
}

internal data class TitleDetailsUiState(
    val load: DetailsLoad = DetailsLoad.Loading,
    val isRefreshing: Boolean = false,
    val isFavorite: Boolean? = null,
    val isRemoveConfirmationShown: Boolean = false,
) {
    val details: TitleDetails? get() = (load as? DetailsLoad.Loaded)?.details
}

internal sealed interface TitleDetailsIntent {
    data class LanguageChanged(val language: Language) : TitleDetailsIntent

    data object BackClicked : TitleDetailsIntent

    data object TrailerClicked : TitleDetailsIntent

    data class RecommendationClicked(val title: Title) : TitleDetailsIntent

    data object Refresh : TitleDetailsIntent

    data object Retry : TitleDetailsIntent

    data object FavoriteClicked : TitleDetailsIntent

    data object RemoveFavoriteConfirmed : TitleDetailsIntent

    data object RemoveFavoriteDismissed : TitleDetailsIntent
}

internal sealed interface TitleDetailsEffect {
    data object NavigateBack : TitleDetailsEffect

    data class OpenTrailer(val url: String) : TitleDetailsEffect

    data class OpenTitle(val title: Title) : TitleDetailsEffect
}
