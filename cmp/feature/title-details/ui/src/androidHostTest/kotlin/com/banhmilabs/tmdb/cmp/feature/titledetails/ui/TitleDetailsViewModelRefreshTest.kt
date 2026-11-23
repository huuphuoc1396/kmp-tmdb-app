package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TitleDetailsViewModelRefreshTest : MainDispatcherTest(StandardTestDispatcher()) {
    private val repository = FakeTitleDetailsRepository(mapOf(CONTRACT_MOVIE_ID to CONTRACT_MOVIE_DETAILS))
    private val viewModel by lazy { TitleDetailsViewModel(CONTRACT_MOVIE_ID, repository, FakeFavoritesRepository()) }
    private val states = mutableListOf<TitleDetailsUiState>()

    @Test
    fun loadingUntilFirstRefreshEnds() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(DetailsLoad.Loading, viewModel.state.value.load)
        advanceUntilIdle()
        assertEquals(DetailsLoad.Loaded(CONTRACT_MOVIE_DETAILS), viewModel.state.value.load)
    }

    @Test
    fun pullToRefreshForcesAndEndsIndicator() = runTest(testDispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repository.refreshRequests.count { it.force })
        assertTrue(states.any { it.isRefreshing })
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullWhileRefreshingIsIgnored() = runTest(testDispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repository.refreshRequests.count { it.force })
    }

    @Test
    fun failedPullKeepsDetails() = runTest(testDispatcher) {
        start()
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(DetailsLoad.Loaded(CONTRACT_MOVIE_DETAILS), viewModel.state.value.load)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullBeforeLanguageDoesNothing() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertTrue(repository.refreshRequests.isEmpty())
    }

    private fun TestScope.start() {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))
        advanceUntilIdle()
    }
}
