package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleDetails

internal sealed interface DetailsLoad {
    /** Nothing cached in the device language and no failure reported. */
    data object Loading : DetailsLoad

    /** Nothing cached in the device language and its last refresh failed. */
    data object Error : DetailsLoad

    /** Cached details; shown even when a later refresh fails. */
    data class Loaded(val details: TitleDetails) : DetailsLoad
}

internal data class TitleDetailsUiState(
    val load: DetailsLoad = DetailsLoad.Loading,
    /** A pull-to-refresh is running; automatic refreshes don't set it. */
    val isRefreshing: Boolean = false,
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
}

internal sealed interface TitleDetailsEffect {
    data object NavigateBack : TitleDetailsEffect

    data class OpenTrailer(val url: String) : TitleDetailsEffect

    data class OpenTitle(val title: Title) : TitleDetailsEffect
}
