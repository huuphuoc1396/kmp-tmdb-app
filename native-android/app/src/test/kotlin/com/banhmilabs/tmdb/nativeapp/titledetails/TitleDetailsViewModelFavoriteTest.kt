package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TitleDetailsViewModelFavoriteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var favorites = FavoritesTestRepository()
    private val viewModel by lazy {
        TitleDetailsViewModel(MOVIE_ID, TitleDetailsTestRepository().mock, favorites.mock)
    }

    @Test
    fun favoriteIsShownFromStorage() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FavoritesTestRepository(isFavorite = true)

        start()

        assertEquals(true, viewModel.state.value.isFavorite)
    }

    @Test
    fun favoriteClickStoresTrue() = runTest(mainDispatcherRule.dispatcher) {
        start()
        assertEquals(false, viewModel.state.value.isFavorite)

        tapFavorite()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertEquals(listOf(true), favorites.writes)
    }

    @Test
    fun secondClickRemovesFavorite() = runTest(mainDispatcherRule.dispatcher) {
        start()

        tapFavorite()
        tapFavorite()

        assertEquals(false, viewModel.state.value.isFavorite)
        assertEquals(listOf(true, false), favorites.writes)
    }

    @Test
    fun quickTapsEndOnLastTap() = runTest(mainDispatcherRule.dispatcher) {
        start()

        listOf(true, false, true).forEach { expected ->
            viewModel.onIntent(TitleDetailsIntent.FavoriteClicked)
            runCurrent()
            assertEquals(expected, viewModel.state.value.isFavorite)
        }
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertEquals(listOf(true, false, true), favorites.writes)
    }

    @Test
    fun favoriteUnchangedByLanguage() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FavoritesTestRepository(isFavorite = true)
        start()

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.PT_BR))
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun clickBeforeFavoriteKnownDoesNothing() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(TitleDetailsIntent.FavoriteClicked)
        advanceUntilIdle()

        assertTrue(favorites.writes.isEmpty())
    }

    private fun TestScope.start() {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.EN_US))
        advanceUntilIdle()
    }

    private fun TestScope.tapFavorite() {
        viewModel.onIntent(TitleDetailsIntent.FavoriteClicked)
        advanceUntilIdle()
    }
}
