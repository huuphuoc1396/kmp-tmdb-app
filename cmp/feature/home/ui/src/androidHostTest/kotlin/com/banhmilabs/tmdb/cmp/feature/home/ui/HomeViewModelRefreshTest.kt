package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
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

/** Pull-to-refresh, on a standard dispatcher so a refresh is still running between two pulls. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelRefreshTest : MainDispatcherTest(StandardTestDispatcher()) {
    private val repository = homeTestRepository()
    private val viewModel by lazy { HomeViewModel(repository, SavedStateHandle()) }
    private val states = mutableListOf<HomeUiState>()

    @Test
    fun pullToRefreshForcesBothListsAndEndsIndicator() = runTest(testDispatcher) {
        start()

        viewModel.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()

        assertEquals(2, repository.refreshRequests.count { it.force })
        assertTrue(states.any { it.isRefreshing })
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullWhileRefreshingIsIgnored() = runTest(testDispatcher) {
        start()

        viewModel.onIntent(HomeIntent.Refresh)
        viewModel.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()

        assertEquals(2, repository.refreshRequests.count { it.force })
    }

    private fun TestScope.start() {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(HomeIntent.LanguageChanged(Language.EN_US))
        advanceUntilIdle()
    }
}
