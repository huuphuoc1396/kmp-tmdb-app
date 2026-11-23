package com.banhmilabs.tmdb.nativeapp.home

import androidx.lifecycle.SavedStateHandle
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelSelectionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository = homeTestRepository()
    private val savedState = SavedStateHandle()

    private val viewModel by lazy { homeViewModel(repository, savedState) }

    @Test
    fun tabSwitchRefreshesWithoutForce() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        val animeRequests = repository.refreshRequests.filter { it.category == TitleCategory.ANIME }
        assertEquals(TitleListType.entries, animeRequests.map { it.listType })
        assertTrue(animeRequests.none { it.force })
    }

    @Test
    fun failureForPreviousTabDoesNotMarkNewTab() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextRefresh(AppError.Network)
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        assertEquals(RowState.Titles(ANIME_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun languageChangeShowsNewLanguageOnly() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel, Language.EN_US)
        repository.failNextRefresh(AppError.Network)

        viewModel.onIntent(HomeIntent.LanguageChanged(Language.PT_BR))

        assertEquals(RowState.Error, viewModel.state.value.featured)
    }

    @Test
    fun listsAreLoadedInTheGivenLanguage() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel, Language.PT_BR)

        assertTrue(repository.refreshRequests.isNotEmpty())
        assertTrue(repository.refreshRequests.all { it.language == Language.PT_BR })
        assertEquals(RowState.Titles(MOVIES_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun selectedCategoryIsRestoredFromSavedState() = runTest(mainDispatcherRule.dispatcher) {
        savedState[SELECTED_CATEGORY_KEY] = TitleCategory.ANIME.name

        start(viewModel)

        assertEquals(TitleCategory.ANIME, viewModel.state.value.selectedCategory)
        assertEquals(RowState.Titles(ANIME_FEATURED), viewModel.state.value.featured)
    }

    @Test
    fun selectingCategorySavesIt() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.SERIES))

        assertEquals(TitleCategory.SERIES.name, savedState.get<String>(SELECTED_CATEGORY_KEY))
    }
}
