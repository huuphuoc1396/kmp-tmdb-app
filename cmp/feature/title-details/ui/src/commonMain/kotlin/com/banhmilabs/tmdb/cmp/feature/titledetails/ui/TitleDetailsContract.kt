package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleDetails

/** [details] is `null` until the title's details are cached in the device language. */
internal data class TitleDetailsUiState(val details: TitleDetails? = null)

internal sealed interface TitleDetailsIntent {
    data class LanguageChanged(val language: Language) : TitleDetailsIntent

    data object BackClicked : TitleDetailsIntent

    data object TrailerClicked : TitleDetailsIntent

    data class RecommendationClicked(val title: Title) : TitleDetailsIntent
}

internal sealed interface TitleDetailsEffect {
    data object NavigateBack : TitleDetailsEffect

    data class OpenTrailer(val url: String) : TitleDetailsEffect

    data class OpenTitle(val title: Title) : TitleDetailsEffect
}
