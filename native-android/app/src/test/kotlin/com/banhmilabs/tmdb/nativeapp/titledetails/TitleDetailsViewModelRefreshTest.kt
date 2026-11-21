package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pull-to-refresh, on a standard dispatcher so a refresh is still running between two pulls. */
@OptIn(ExperimentalCoroutinesApi::class)
class TitleDetailsViewModelRefreshTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TitleDetailsTestRepository()
    private val viewModel by lazy {
        TitleDetailsViewModel(MOVIE_ID, repository.mock, FavoritesTestRepository().mock)
    }
    private val states = mutableListOf<TitleDetailsUiState>()

    @Test
    fun loadingUntilFirstRefreshEnds() = runTest(mainDispatcherRule.dispatcher) {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(DetailsLoad.Loading, viewModel.state.value.load)
        advanceUntilIdle()
        assertEquals(DetailsLoad.Loaded(MOVIE_DETAILS), viewModel.state.value.load)
    }

    @Test
    fun pullToRefreshForcesAndEndsIndicator() = runTest(mainDispatcherRule.dispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repository.refreshRequests.count { it.force })
        assertTrue(states.any { it.isRefreshing })
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullWhileRefreshingIsIgnored() = runTest(mainDispatcherRule.dispatcher) {
        start()

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repository.refreshRequests.count { it.force })
    }

    @Test
    fun failedPullKeepsDetails() = runTest(mainDispatcherRule.dispatcher) {
        start()
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(TitleDetailsIntent.Refresh)
        advanceUntilIdle()

        assertEquals(DetailsLoad.Loaded(MOVIE_DETAILS), viewModel.state.value.load)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullBeforeLanguageDoesNothing() = runTest(mainDispatcherRule.dispatcher) {
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
