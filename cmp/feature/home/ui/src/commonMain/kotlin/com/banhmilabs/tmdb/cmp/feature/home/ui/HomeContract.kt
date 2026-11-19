package com.banhmilabs.tmdb.cmp.feature.home.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleCategory

internal data class HomeUiState(
    val selectedCategory: TitleCategory = TitleCategory.MOVIES,
    val featured: List<Title> = emptyList(),
    val popular: List<Title> = emptyList(),
)

internal sealed interface HomeIntent {
    data class LanguageChanged(val language: Language) : HomeIntent

    data class CategorySelected(val category: TitleCategory) : HomeIntent

    data class TitleClicked(val title: Title) : HomeIntent
}

internal sealed interface HomeEffect {
    data class OpenTitle(val title: Title) : HomeEffect
}
