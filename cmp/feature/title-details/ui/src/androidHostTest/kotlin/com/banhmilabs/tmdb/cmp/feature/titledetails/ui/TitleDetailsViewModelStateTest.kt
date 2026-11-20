package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.common.AppError
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

/** Loading, error, retry and cached details. */
class TitleDetailsViewModelStateTest : MainDispatcherTest() {
    private val repository = FakeTitleDetailsRepository(mapOf(CONTRACT_MOVIE_ID to CONTRACT_MOVIE_DETAILS))
    private val viewModel by lazy { TitleDetailsViewModel(CONTRACT_MOVIE_ID, repository) }
    private val states = mutableListOf<TitleDetailsUiState>()

    @Test
    fun startsLoading() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { states += it } }

        assertEquals(DetailsLoad.Loading, viewModel.state.value.load)
        assertEquals(false, viewModel.state.value.isRefreshing)
    }

    @Test
    fun failedLoadWithoutCacheShowsError() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)

        start()

        assertEquals(DetailsLoad.Error, viewModel.state.value.load)
    }

    @Test
    fun missingTitleShowsError() = runTest(testDispatcher) {
        val missing = TitleDetailsViewModel(CONTRACT_MOVIE_ID, FakeTitleDetailsRepository())
        backgroundScope.launch { missing.state.collect {} }

        missing.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(DetailsLoad.Error, missing.state.value.load)
    }

    @Test
    fun retryLoadsAgain() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)
        start()

        viewModel.onIntent(TitleDetailsIntent.Retry)

        assertEquals(DetailsLoad.Loaded(CONTRACT_MOVIE_DETAILS), viewModel.state.value.load)
        assertTrue(repository.refreshRequests.last().force)
    }

    @Test
    fun failedRefreshKeepsCachedDetails() = runTest(testDispatcher) {
        start()
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(TitleDetailsIntent.Retry)

        assertEquals(DetailsLoad.Loaded(CONTRACT_MOVIE_DETAILS), viewModel.state.value.load)
    }

    @Test
    fun errorInOneLanguageIsNotShownInTheOther() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(Language.PT_BR)
        assertEquals(DetailsLoad.Error, viewModel.state.value.load)
        states.clear()

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(DetailsLoad.Loaded(CONTRACT_MOVIE_DETAILS), viewModel.state.value.load)
        assertTrue(states.none { it.load == DetailsLoad.Error })
    }

    private fun TestScope.start(language: Language = Language.EN_US) {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(language))
    }
}
