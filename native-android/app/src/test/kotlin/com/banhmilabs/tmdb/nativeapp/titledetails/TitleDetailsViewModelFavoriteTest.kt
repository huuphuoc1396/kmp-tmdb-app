package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class TitleDetailsViewModelFavoriteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var favorites = FakeFavoritesRepository()
    private val viewModel by lazy {
        TitleDetailsViewModel(MOVIE_ID, titleDetailsTestRepository(), favorites)
    }

    @Test
    fun favoriteIsShownFromStorage() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))

        start()

        assertEquals(true, viewModel.state.value.isFavorite)
    }

    @Test
    fun favoriteClickStoresTrue() = runTest(mainDispatcherRule.dispatcher) {
        start()
        assertEquals(false, viewModel.state.value.isFavorite)

        tapFavorite()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertEquals(listOf(true), favorites.writes.map { it.second })
    }

    @Test
    fun clickOnFavoriteAsksToConfirm() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
        start()

        tapFavorite()

        assertTrue(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun confirmRemovesFavorite() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(false, viewModel.state.value.isFavorite)
        assertEquals(listOf(MOVIE_ID to false), favorites.writes)
    }

    @Test
    fun confirmTwiceWritesOnce() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf(MOVIE_ID to false), favorites.writes)
    }

    @Test
    fun dismissKeepsFavorite() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteDismissed)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun quickTapsOnFavoriteKeepOneConfirmation() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
        start()

        repeat(3) { viewModel.onIntent(TitleDetailsIntent.FavoriteClicked) }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun favoriteUnchangedByLanguage() = runTest(mainDispatcherRule.dispatcher) {
        favorites = FakeFavoritesRepository(setOf(MOVIE_ID))
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
