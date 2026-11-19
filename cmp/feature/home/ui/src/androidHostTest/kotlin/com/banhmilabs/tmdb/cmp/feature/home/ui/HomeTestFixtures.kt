package com.banhmilabs.tmdb.cmp.feature.home.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.fake.FakeTitleListsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

internal val MOVIES_FEATURED = listOf(testTitle(1, TitleType.MOVIE))
internal val MOVIES_POPULAR = listOf(testTitle(2, TitleType.MOVIE), testTitle(3, TitleType.MOVIE))
internal val ANIME_FEATURED = listOf(testTitle(4, TitleType.TV))
internal val ANIME_POPULAR = listOf(testTitle(5, TitleType.TV))

internal fun testTitle(id: Long, type: TitleType) = Title(TitleId(id, type), name = "Title $id", posterUrl = null)

/** A fake whose Movies and Anime lists have distinct titles; other lists are empty. */
internal fun homeTestRepository() = FakeTitleListsRepository().apply {
    setRemoteTitles(TitleCategory.MOVIES, TitleListType.FEATURED, MOVIES_FEATURED)
    setRemoteTitles(TitleCategory.MOVIES, TitleListType.POPULAR, MOVIES_POPULAR)
    setRemoteTitles(TitleCategory.ANIME, TitleListType.FEATURED, ANIME_FEATURED)
    setRemoteTitles(TitleCategory.ANIME, TitleListType.POPULAR, ANIME_POPULAR)
}

/** Keeps [viewModel]'s state subscribed for the whole test and reports the device [language]. */
internal fun TestScope.start(viewModel: HomeViewModel, language: Language = Language.EN_US) {
    backgroundScope.launch { viewModel.state.collect {} }
    viewModel.onIntent(HomeIntent.LanguageChanged(language))
}
