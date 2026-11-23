package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_ID
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

@OptIn(ExperimentalCoroutinesApi::class)
class TitleDetailsViewModelFavoriteTest : MainDispatcherTest(StandardTestDispatcher()) {
    private val repository = FakeTitleDetailsRepository(mapOf(CONTRACT_MOVIE_ID to CONTRACT_MOVIE_DETAILS))
    private var favorites = FakeFavoritesRepository()
    private val viewModel by lazy { TitleDetailsViewModel(CONTRACT_MOVIE_ID, repository, favorites) }

    @Test
    fun favoriteIsShownFromStorage() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))

        start()

        assertEquals(true, viewModel.state.value.isFavorite)
    }

    @Test
    fun favoriteClickStoresTrue() = runTest(testDispatcher) {
        start()
        assertEquals(false, viewModel.state.value.isFavorite)

        tapFavorite()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertEquals(listOf(CONTRACT_MOVIE_ID to true), favorites.writes)
    }

    @Test
    fun clickOnFavoriteAsksToConfirm() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()

        tapFavorite()

        assertTrue(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun confirmRemovesFavorite() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(false, viewModel.state.value.isFavorite)
        assertEquals(listOf(CONTRACT_MOVIE_ID to false), favorites.writes)
    }

    @Test
    fun confirmTwiceWritesOnce() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf(CONTRACT_MOVIE_ID to false), favorites.writes)
    }

    @Test
    fun dismissKeepsFavorite() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()
        tapFavorite()

        viewModel.onIntent(TitleDetailsIntent.RemoveFavoriteDismissed)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun quickTapsOnFavoriteKeepOneConfirmation() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()

        repeat(3) { viewModel.onIntent(TitleDetailsIntent.FavoriteClicked) }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isRemoveConfirmationShown)
        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun favoriteUnchangedByLanguage() = runTest(testDispatcher) {
        favorites = FakeFavoritesRepository(setOf(CONTRACT_MOVIE_ID))
        start()

        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(Language.PT_BR))
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertTrue(favorites.writes.isEmpty())
    }

    @Test
    fun clickBeforeFavoriteKnownDoesNothing() = runTest(testDispatcher) {
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
