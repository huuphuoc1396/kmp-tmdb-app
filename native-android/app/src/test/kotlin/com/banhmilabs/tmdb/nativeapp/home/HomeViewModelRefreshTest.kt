package com.banhmilabs.tmdb.nativeapp.home

import androidx.lifecycle.SavedStateHandle
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelRefreshTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = HomeTestRepository()
    private val viewModel by lazy { HomeViewModel(repository.mock, SavedStateHandle()) }
    private val states = mutableListOf<HomeUiState>()

    @Test
    fun pullToRefreshForcesBothListsAndEndsIndicator() = runTest(mainDispatcherRule.dispatcher) {
        start()

        viewModel.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()

        assertEquals(2, repository.refreshRequests.count { it.force })
        assertTrue(states.any { it.isRefreshing })
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun pullWhileRefreshingIsIgnored() = runTest(mainDispatcherRule.dispatcher) {
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
