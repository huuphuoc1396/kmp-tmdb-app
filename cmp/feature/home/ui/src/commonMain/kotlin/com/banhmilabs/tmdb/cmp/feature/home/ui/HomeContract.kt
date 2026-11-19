package com.banhmilabs.tmdb.cmp.feature.home.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType

/** What a poster row shows. */
internal sealed interface RowState {
    /** Nothing cached yet and no failure reported. */
    data object Loading : RowState

    /** Nothing cached and the last refresh failed. */
    data object Error : RowState

    /** Cached titles; an empty list means the row loaded with nothing in it. */
    data class Titles(val titles: List<Title>) : RowState
}

internal data class HomeUiState(
    val selectedCategory: TitleCategory = TitleCategory.MOVIES,
    val featured: RowState = RowState.Loading,
    val popular: RowState = RowState.Loading,
    /** A pull-to-refresh is running; automatic refreshes don't set it. */
    val isRefreshing: Boolean = false,
)

internal sealed interface HomeIntent {
    data class LanguageChanged(val language: Language) : HomeIntent

    data class CategorySelected(val category: TitleCategory) : HomeIntent

    data class TitleClicked(val title: Title) : HomeIntent

    data object Refresh : HomeIntent

    data class Retry(val listType: TitleListType) : HomeIntent
}

internal sealed interface HomeEffect {
    data class OpenTitle(val title: Title) : HomeEffect
}
