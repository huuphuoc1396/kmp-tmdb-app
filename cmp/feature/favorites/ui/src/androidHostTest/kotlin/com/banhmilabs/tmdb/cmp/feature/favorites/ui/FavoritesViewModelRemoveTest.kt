package com.banhmilabs.tmdb.cmp.feature.favorites.ui

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.ObserveFavoriteTitles
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoritesViewModelRemoveTest : MainDispatcherTest() {
    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { FavoritesViewModel(ObserveFavoriteTitles(favorites, details), favorites) }

    @Test
    fun removeClickAsksToConfirm() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start()
        val favorite = viewModel.state.value.favorites.single()

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(favorite))

        assertEquals(favorite, viewModel.state.value.pendingRemoval)
        assertEquals(1, viewModel.state.value.favorites.size)
    }

    @Test
    fun secondRemoveClickKeepsFirstRequest() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start()
        val (series, movie) = viewModel.state.value.favorites

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(series))
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(movie))

        assertEquals(series, viewModel.state.value.pendingRemoval)
    }

    @Test
    fun confirmRemovesFavoriteAndCloses() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start()
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.first()))

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteConfirmed)

        assertNull(viewModel.state.value.pendingRemoval)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
    }

    @Test
    fun confirmTwiceWritesOnce() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start()
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteConfirmed)
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteConfirmed)

        assertEquals(listOf(MOVIE to true, MOVIE to false), favorites.writes)
    }

    @Test
    fun dismissKeepsFavorite() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start()
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteDismissed)

        assertNull(viewModel.state.value.pendingRemoval)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
        assertEquals(listOf(MOVIE to true), favorites.writes)
    }

    @Test
    fun removingLastFavoriteLeavesEmptyList() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start()
        viewModel.onIntent(FavoritesIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(FavoritesIntent.RemoveFavoriteConfirmed)

        assertEquals(FavoritesUiState(), viewModel.state.value)
    }

    private fun TestScope.start(language: Language = Language.EN_US) {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onIntent(FavoritesIntent.LanguageChanged(language))
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
        val SERIES = TitleId(1399, TitleType.TV)
    }
}
