package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitleDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository = TitleDetailsTestRepository()

    private val viewModel by lazy {
        TitleDetailsViewModel(MOVIE_ID, repository.mock, FavoritesTestRepository().mock)
    }
    private val effects = mutableListOf<TitleDetailsEffect>()

    @Test
    fun nothingObservedBeforeLanguage() = runTest(mainDispatcherRule.dispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }

        assertEquals(TitleDetailsUiState(), viewModel.state.value)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun showsDetailsAfterLanguage() = runTest(mainDispatcherRule.dispatcher) {
        start()

        assertEquals(
            TitleDetailsUiState(DetailsLoad.Loaded(MOVIE_DETAILS), isFavorite = false),
            viewModel.state.value,
        )
    }

    @Test
    fun refreshesOncePerLanguage() = runTest(mainDispatcherRule.dispatcher) {
        start(Language.PT_BR)
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.PT_BR))
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(
            listOf(DetailsRefresh(Language.PT_BR, force = false), DetailsRefresh(Language.EN_US, force = false)),
            repository.refreshRequests,
        )
    }

    @Test
    fun languageChangeShowsOtherLanguage() = runTest(mainDispatcherRule.dispatcher) {
        start(Language.PT_BR)
        val english = MOVIE_DETAILS.copy(name = "English name")
        repository.remoteDetails = english

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(english, viewModel.state.value.details)
    }

    @Test
    fun trailerClickOpensTrailerUrl() = runTest(mainDispatcherRule.dispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.TrailerClicked)

        val trailer = TitleDetailsEffect.OpenTrailer("https://www.youtube.com/watch?v=BdJKm16Co6M")
        assertEquals(listOf<TitleDetailsEffect>(trailer), effects)
    }

    @Test
    fun trailerClickWithoutTrailerDoesNothing() = runTest(mainDispatcherRule.dispatcher) {
        repository.remoteDetails = MOVIE_DETAILS.copy(trailer = null)
        start()

        viewModel.onIntent(TitleDetailsIntent.TrailerClicked)

        assertTrue(effects.isEmpty())
    }

    @Test
    fun recommendationClickOpensTitle() = runTest(mainDispatcherRule.dispatcher) {
        start()
        val recommendation = MOVIE_DETAILS.recommendations.first()

        viewModel.onIntent(TitleDetailsIntent.RecommendationClicked(recommendation))

        assertEquals(listOf<TitleDetailsEffect>(TitleDetailsEffect.OpenTitle(recommendation)), effects)
    }

    @Test
    fun backClickNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.BackClicked)

        assertEquals(listOf<TitleDetailsEffect>(TitleDetailsEffect.NavigateBack), effects)
    }

    private fun TestScope.start(language: Language = Language.EN_US) {
        backgroundScope.launch { viewModel.state.collect {} }
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(language))
    }
}
