package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.common.AppError
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

/** Loading, error, retry and cached details. */
class TitleDetailsViewModelStateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository = TitleDetailsTestRepository()
    private val viewModel by lazy {
        TitleDetailsViewModel(MOVIE_ID, repository.mock, FavoritesTestRepository().mock)
    }
    private val states = mutableListOf<TitleDetailsUiState>()

    @Test
    fun startsLoading() = runTest(mainDispatcherRule.dispatcher) {
        backgroundScope.launch { viewModel.state.collect { states += it } }

        assertEquals(DetailsLoad.Loading, viewModel.state.value.load)
        assertEquals(false, viewModel.state.value.isRefreshing)
    }

    @Test
    fun failedLoadWithoutCacheShowsError() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)

        start()

        assertEquals(DetailsLoad.Error, viewModel.state.value.load)
    }

    @Test
    fun missingTitleShowsError() = runTest(mainDispatcherRule.dispatcher) {
        repository.remoteDetails = null

        start()

        assertEquals(DetailsLoad.Error, viewModel.state.value.load)
    }

    @Test
    fun retryLoadsAgain() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)
        start()

        viewModel.onIntent(TitleDetailsIntent.Retry)

        assertEquals(DetailsLoad.Loaded(MOVIE_DETAILS), viewModel.state.value.load)
        assertTrue(repository.refreshRequests.last().force)
    }

    @Test
    fun failedRefreshKeepsCachedDetails() = runTest(mainDispatcherRule.dispatcher) {
        start()
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(TitleDetailsIntent.Retry)

        assertEquals(DetailsLoad.Loaded(MOVIE_DETAILS), viewModel.state.value.load)
    }

    @Test
    fun errorInOneLanguageIsNotShownInTheOther() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(Language.PT_BR)
        assertEquals(DetailsLoad.Error, viewModel.state.value.load)
        states.clear()

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))

        assertEquals(DetailsLoad.Loaded(MOVIE_DETAILS), viewModel.state.value.load)
        assertTrue(states.none { it.load == DetailsLoad.Error })
    }

    private fun TestScope.start(language: Language = Language.EN_US) {
        backgroundScope.launch { viewModel.state.collect { states += it } }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(language))
    }
}
