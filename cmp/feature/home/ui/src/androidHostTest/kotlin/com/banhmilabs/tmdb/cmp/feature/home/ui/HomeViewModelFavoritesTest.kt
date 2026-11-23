package com.banhmilabs.tmdb.cmp.feature.home.ui

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelFavoritesTest : MainDispatcherTest() {
    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { homeViewModel(homeTestRepository(), favorites = favorites, details = details) }

    @Test
    fun noFavoritesGivesEmptySection() = runTest(testDispatcher) {
        start(viewModel)

        assertEquals(emptyList(), viewModel.state.value.favorites)
        assertEquals(0, viewModel.state.value.favoritesCount)
    }

    @Test
    fun showsTenNewestFavoritesWithTotalCount() = runTest(testDispatcher) {
        val ids = (1L..12L).map { TitleId(it, TitleType.MOVIE) }
        ids.forEach { favorites.setFavorite(it, isFavorite = true) }

        start(viewModel)

        assertEquals(ids.reversed().take(HOME_FAVORITES_LIMIT), viewModel.state.value.favorites.map { it.id })
        assertEquals(12, viewModel.state.value.favoritesCount)
    }

    @Test
    fun sectionIsTheSameForEveryCategory() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        val before = viewModel.state.value.favorites

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        assertEquals(before, viewModel.state.value.favorites)
        assertEquals(1, viewModel.state.value.favoritesCount)
    }

    @Test
    fun favoritesUpdateWhenOneIsAddedOrRemoved() = runTest(testDispatcher) {
        start(viewModel)

        favorites.setFavorite(MOVIE, isFavorite = true)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })

        favorites.setFavorite(MOVIE, isFavorite = false)
        assertEquals(0, viewModel.state.value.favoritesCount)
    }

    @Test
    fun favoritesFollowLanguageChange() = runTest(testDispatcher) {
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS.copy(name = "Clube da Luta"))
        details.refresh(MOVIE, Language.PT_BR)
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS)
        details.refresh(MOVIE, Language.EN_US)
        favorites.setFavorite(MOVIE, isFavorite = true)

        start(viewModel, Language.PT_BR)
        assertEquals("Clube da Luta", viewModel.state.value.favorites.single().summary?.name)

        viewModel.onIntent(HomeIntent.LanguageChanged(Language.EN_US))
        assertEquals("Fight Club", viewModel.state.value.favorites.single().summary?.name)
    }

    @Test
    fun favoriteClickOpensFavorite() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.FavoriteClicked(MOVIE))

            assertEquals(HomeEffect.OpenFavorite(MOVIE), awaitItem())
        }
    }

    @Test
    fun seeAllOpensFavorites() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.SeeAllFavoritesClicked)

            assertEquals(HomeEffect.OpenFavorites, awaitItem())
        }
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
    }
}
