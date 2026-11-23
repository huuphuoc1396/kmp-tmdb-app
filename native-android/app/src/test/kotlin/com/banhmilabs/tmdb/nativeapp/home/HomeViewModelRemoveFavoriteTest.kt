package com.banhmilabs.tmdb.nativeapp.home

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeViewModelRemoveFavoriteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { homeViewModel(homeTestRepository(), favorites = favorites, details = details) }

    @Test
    fun removeClickAsksToConfirm() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        val favorite = viewModel.state.value.favorites.single()

        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(favorite))

        assertEquals(favorite, viewModel.state.value.pendingRemoval)
        assertEquals(1, viewModel.state.value.favoritesCount)
    }

    @Test
    fun secondRemoveClickKeepsFirstRequest() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start(viewModel)
        val (series, movie) = viewModel.state.value.favorites

        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(series))
        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(movie))

        assertEquals(series, viewModel.state.value.pendingRemoval)
    }

    @Test
    fun confirmRemovesFavoriteAndCloses() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start(viewModel)
        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.first()))

        viewModel.onIntent(HomeIntent.RemoveFavoriteConfirmed)

        assertNull(viewModel.state.value.pendingRemoval)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
        assertEquals(1, viewModel.state.value.favoritesCount)
    }

    @Test
    fun confirmTwiceWritesOnce() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(HomeIntent.RemoveFavoriteConfirmed)
        viewModel.onIntent(HomeIntent.RemoveFavoriteConfirmed)

        assertEquals(listOf(MOVIE to true, MOVIE to false), favorites.writes)
    }

    @Test
    fun dismissKeepsFavorite() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(HomeIntent.RemoveFavoriteDismissed)

        assertNull(viewModel.state.value.pendingRemoval)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
        assertEquals(listOf(MOVIE to true), favorites.writes)
    }

    @Test
    fun removingLastFavoriteHidesSection() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        viewModel.onIntent(HomeIntent.RemoveFavoriteClicked(viewModel.state.value.favorites.single()))

        viewModel.onIntent(HomeIntent.RemoveFavoriteConfirmed)

        assertEquals(0, viewModel.state.value.favoritesCount)
        assertEquals(emptyList(), viewModel.state.value.favorites)
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
        val SERIES = TitleId(1399, TitleType.TV)
    }
}
