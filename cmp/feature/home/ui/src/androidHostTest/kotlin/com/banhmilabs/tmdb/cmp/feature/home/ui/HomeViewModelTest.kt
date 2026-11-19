package com.banhmilabs.tmdb.cmp.feature.home.ui

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.fake.FakeTitleListsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeViewModelTest : MainDispatcherTest() {
    private val repository = FakeTitleListsRepository().apply {
        setRemoteTitles(TitleCategory.MOVIES, TitleListType.FEATURED, MOVIES_FEATURED)
        setRemoteTitles(TitleCategory.MOVIES, TitleListType.POPULAR, MOVIES_POPULAR)
        setRemoteTitles(TitleCategory.ANIME, TitleListType.FEATURED, ANIME_FEATURED)
        setRemoteTitles(TitleCategory.ANIME, TitleListType.POPULAR, ANIME_POPULAR)
    }

    // Created after MainDispatcherTest sets the Main dispatcher: the ViewModel starts collecting in its init.
    private val viewModel by lazy { HomeViewModel(repository) }

    @Test
    fun startsWithMoviesSelectedAndNoTitles() {
        assertEquals(HomeUiState(selectedCategory = TitleCategory.MOVIES), viewModel.state.value)
    }

    @Test
    fun languageKnownShowsMoviesFeaturedAndPopular() = runTest(testDispatcher) {
        viewModel.state.test {
            skipItems(1)
            viewModel.onIntent(HomeIntent.LanguageChanged(Language.EN_US))

            assertEquals(
                HomeUiState(TitleCategory.MOVIES, featured = MOVIES_FEATURED, popular = MOVIES_POPULAR),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun selectingCategoryShowsItsLists() = runTest(testDispatcher) {
        viewModel.state.test {
            viewModel.onIntent(HomeIntent.LanguageChanged(Language.EN_US))
            viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

            assertEquals(
                HomeUiState(TitleCategory.ANIME, featured = ANIME_FEATURED, popular = ANIME_POPULAR),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun listsAreLoadedInTheGivenLanguage() = runTest(testDispatcher) {
        viewModel.state.test {
            viewModel.onIntent(HomeIntent.LanguageChanged(Language.PT_BR))
            cancelAndIgnoreRemainingEvents()
        }

        assertNotNull(repository.observe(TitleCategory.MOVIES, TitleListType.FEATURED, Language.PT_BR).first())
        assertNotNull(repository.observe(TitleCategory.MOVIES, TitleListType.POPULAR, Language.PT_BR).first())
        assertNull(repository.observe(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US).first())
    }

    @Test
    fun titleClickedOpensThatTitle() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.TitleClicked(MOVIES_FEATURED.first()))

            assertEquals(HomeEffect.OpenTitle(MOVIES_FEATURED.first()), awaitItem())
        }
    }

    private companion object {
        val MOVIES_FEATURED = listOf(title(1, TitleType.MOVIE))
        val MOVIES_POPULAR = listOf(title(2, TitleType.MOVIE), title(3, TitleType.MOVIE))
        val ANIME_FEATURED = listOf(title(4, TitleType.TV))
        val ANIME_POPULAR = listOf(title(5, TitleType.TV))

        fun title(id: Long, type: TitleType) = Title(TitleId(id, type), name = "Title $id", posterUrl = null)
    }
}
