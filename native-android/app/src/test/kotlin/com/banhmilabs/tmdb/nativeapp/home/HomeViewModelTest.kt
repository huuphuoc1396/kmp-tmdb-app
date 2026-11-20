package com.banhmilabs.tmdb.nativeapp.home

import androidx.lifecycle.SavedStateHandle
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository = HomeTestRepository()
    private val savedState = SavedStateHandle()

    // Created after MainDispatcherRule sets the Main dispatcher: the ViewModel starts collecting in its init.
    private val viewModel by lazy { HomeViewModel(repository.mock, savedState) }

    @Test
    fun startsLoadingWithMoviesSelected() {
        assertEquals(HomeUiState(selectedCategory = TitleCategory.MOVIES), viewModel.state.value)
    }

    @Test
    fun rowsShowTitlesOnceLoaded() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        assertEquals(
            HomeUiState(
                selectedCategory = TitleCategory.MOVIES,
                featured = RowState.Titles(MOVIES_FEATURED),
                popular = RowState.Titles(MOVIES_POPULAR),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun emptyListShowsEmptyRow() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.DOCUMENTARIES))

        assertEquals(RowState.Titles(emptyList()), viewModel.state.value.featured)
    }

    @Test
    fun failedLoadWithoutCacheShowsError() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)

        start(viewModel)

        assertEquals(RowState.Error, viewModel.state.value.featured)
    }

    @Test
    fun oneRowFailsWhileOtherShowsTitles() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)

        start(viewModel)

        assertEquals(RowState.Error, viewModel.state.value.featured)
        assertEquals(RowState.Titles(MOVIES_POPULAR), viewModel.state.value.popular)
    }

    @Test
    fun failedRefreshWithCacheKeepsTitles() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(HomeIntent.Refresh)

        assertEquals(RowState.Titles(MOVIES_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun retryReloadsTheFailedRow() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(viewModel)

        viewModel.onIntent(HomeIntent.Retry(TitleListType.FEATURED))

        assertEquals(RowState.Titles(MOVIES_FEATURED), viewModel.state.value.featured)
        val retry = repository.refreshRequests.last()
        assertEquals(TitleListType.FEATURED, retry.listType)
        assertTrue(retry.force)
    }

    @Test
    fun titleClickedOpensThatTitle() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(HomeIntent.TitleClicked(MOVIES_FEATURED.first()))

        assertEquals(HomeEffect.OpenTitle(MOVIES_FEATURED.first()), viewModel.effects.first())
    }
}
