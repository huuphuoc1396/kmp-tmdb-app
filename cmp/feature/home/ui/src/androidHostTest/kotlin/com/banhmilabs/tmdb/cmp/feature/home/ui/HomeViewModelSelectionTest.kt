package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelSelectionTest : MainDispatcherTest() {
    private val repository = homeTestRepository()
    private val savedState = SavedStateHandle()

    // Created after MainDispatcherTest sets the Main dispatcher: the ViewModel starts collecting in its init.
    private val viewModel by lazy { HomeViewModel(repository, savedState) }

    @Test
    fun tabSwitchRefreshesWithoutForce() = runTest(testDispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        val animeRequests = repository.refreshRequests.filter { it.category == TitleCategory.ANIME }
        assertEquals(TitleListType.entries, animeRequests.map { it.listType })
        assertTrue(animeRequests.none { it.force })
    }

    @Test
    fun failureForPreviousTabDoesNotMarkNewTab() = runTest(testDispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        assertEquals(RowState.Titles(ANIME_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun languageChangeShowsNewLanguageOnly() = runTest(testDispatcher) {
        start(viewModel, Language.EN_US)
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(HomeIntent.LanguageChanged(Language.PT_BR))

        assertEquals(RowState.Error, viewModel.state.value.featured)
    }

    @Test
    fun selectedCategoryIsRestoredFromSavedState() = runTest(testDispatcher) {
        savedState[SELECTED_CATEGORY_KEY] = TitleCategory.ANIME.name

        start(viewModel)

        assertEquals(TitleCategory.ANIME, viewModel.state.value.selectedCategory)
        assertEquals(RowState.Titles(ANIME_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun selectingCategorySavesIt() = runTest(testDispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.SERIES))

        assertEquals(TitleCategory.SERIES.name, savedState.get<String>(SELECTED_CATEGORY_KEY))
    }
}
