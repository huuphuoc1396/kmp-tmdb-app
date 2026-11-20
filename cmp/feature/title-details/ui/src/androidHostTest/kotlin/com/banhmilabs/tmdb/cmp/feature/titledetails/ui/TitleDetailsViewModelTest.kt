package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_ID
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitleDetailsViewModelTest : MainDispatcherTest() {
    private val repository = FakeTitleDetailsRepository(mapOf(CONTRACT_MOVIE_ID to CONTRACT_MOVIE_DETAILS))

    // Created after MainDispatcherTest sets the Main dispatcher: the ViewModel starts collecting in its init.
    private val viewModel by lazy { TitleDetailsViewModel(CONTRACT_MOVIE_ID, repository) }

    @Test
    fun nothingObservedBeforeLanguage() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }

        assertEquals(TitleDetailsUiState(), viewModel.state.value)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun showsDetailsAfterLanguage() = runTest(testDispatcher) {
        start()

        assertEquals(TitleDetailsUiState(CONTRACT_MOVIE_DETAILS), viewModel.state.value)
    }

    @Test
    fun refreshesOncePerLanguage() = runTest(testDispatcher) {
        start(Language.PT_BR)
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.PT_BR))
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(
            listOf(
                FakeTitleDetailsRepository.RefreshRequest(CONTRACT_MOVIE_ID, Language.PT_BR, force = false),
                FakeTitleDetailsRepository.RefreshRequest(CONTRACT_MOVIE_ID, Language.EN_US, force = false),
            ),
            repository.refreshRequests,
        )
    }

    @Test
    fun languageChangeShowsOtherLanguage() = runTest(testDispatcher) {
        start(Language.PT_BR)
        val english = CONTRACT_MOVIE_DETAILS.copy(name = "English name")
        repository.setRemoteDetails(CONTRACT_MOVIE_ID, english)

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(english, viewModel.state.value.details)
    }

    @Test
    fun trailerClickOpensTrailerUrl() = runTest(testDispatcher) {
        start()

        viewModel.effects.test {
            viewModel.onIntent(TitleDetailsIntent.TrailerClicked)

            assertEquals(
                TitleDetailsEffect.OpenTrailer("https://www.youtube.com/watch?v=BdJKm16Co6M"),
                awaitItem(),
            )
        }
    }

    @Test
    fun trailerClickWithoutTrailerDoesNothing() = runTest(testDispatcher) {
        repository.setRemoteDetails(CONTRACT_MOVIE_ID, CONTRACT_MOVIE_DETAILS.copy(trailer = null))
        start()

        viewModel.effects.test {
            viewModel.onIntent(TitleDetailsIntent.TrailerClicked)

            expectNoEvents()
        }
    }

    @Test
    fun recommendationClickOpensTitle() = runTest(testDispatcher) {
        val recommendation = CONTRACT_MOVIE_DETAILS.recommendations.first()

        viewModel.effects.test {
            viewModel.onIntent(TitleDetailsIntent.RecommendationClicked(recommendation))

            assertEquals(TitleDetailsEffect.OpenTitle(recommendation), awaitItem())
        }
    }

    @Test
    fun backClickNavigatesBack() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(TitleDetailsIntent.BackClicked)

            assertEquals(TitleDetailsEffect.NavigateBack, awaitItem())
        }
    }

    /** Keeps the state subscribed for the whole test and reports the device [language]. */
    private fun TestScope.start(language: Language = Language.EN_US) {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(language))
    }
}
