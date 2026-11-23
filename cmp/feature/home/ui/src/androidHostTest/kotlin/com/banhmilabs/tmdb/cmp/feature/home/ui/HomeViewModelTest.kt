package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest : MainDispatcherTest() {
    private val repository = homeTestRepository()
    private val savedState = SavedStateHandle()

    private val viewModel by lazy { HomeViewModel(repository, savedState) }

    @Test
    fun startsLoadingWithMoviesSelected() {
        assertEquals(HomeUiState(selectedCategory = TitleCategory.MOVIES), viewModel.state.value)
    }

    @Test
    fun rowsShowTitlesOnceLoaded() = runTest(testDispatcher) {
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
    fun emptyListShowsEmptyRow() = runTest(testDispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.DOCUMENTARIES))

        assertEquals(RowState.Titles(emptyList()), viewModel.state.value.featured)
    }

    @Test
    fun failedLoadWithoutCacheShowsError() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)

        start(viewModel)

        assertEquals(RowState.Error, viewModel.state.value.featured)
    }

    @Test
    fun oneRowFailsWhileOtherShowsTitles() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)

        start(viewModel)

        assertEquals(RowState.Error, viewModel.state.value.featured)
        assertEquals(RowState.Titles(MOVIES_POPULAR), viewModel.state.value.popular)
    }

    @Test
    fun failedRefreshWithCacheKeepsTitles() = runTest(testDispatcher) {
        start(viewModel)
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(HomeIntent.Refresh)

        assertEquals(RowState.Titles(MOVIES_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun retryReloadsTheFailedRow() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(viewModel)

        viewModel.onIntent(HomeIntent.Retry(TitleListType.FEATURED))

        assertEquals(RowState.Titles(MOVIES_FEATURED), viewModel.state.value.featured)
        val retry = repository.refreshRequests.last()
        assertEquals(TitleListType.FEATURED, retry.listType)
        assertTrue(retry.force)
    }

    @Test
    fun titleClickedOpensThatTitle() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.TitleClicked(MOVIES_FEATURED.first()))

            assertEquals(HomeEffect.OpenTitle(MOVIES_FEATURED.first()), awaitItem())
        }
    }
}
