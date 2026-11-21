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
import kotlin.test.assertTrue

/** The heart, on a standard dispatcher so each tap is checked after its write ran. */
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
    fun secondClickRemovesFavorite() = runTest(testDispatcher) {
        start()

        tapFavorite()
        tapFavorite()

        assertEquals(false, viewModel.state.value.isFavorite)
        assertEquals(listOf(CONTRACT_MOVIE_ID to true, CONTRACT_MOVIE_ID to false), favorites.writes)
    }

    @Test
    fun quickTapsEndOnLastTap() = runTest(testDispatcher) {
        start()

        listOf(true, false, true).forEach { expected ->
            viewModel.onIntent(TitleDetailsIntent.FavoriteClicked)
            testScheduler.runCurrent()
            assertEquals(expected, viewModel.state.value.isFavorite)
        }
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isFavorite)
        assertEquals(listOf(true, false, true), favorites.writes.map { it.second })
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
