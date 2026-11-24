package com.banhmilabs.tmdb.nativeapp.home

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelFavoritesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { homeViewModel(homeTestRepository(), favorites = favorites, details = details) }

    @Test
    fun noFavoritesGivesEmptySection() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        assertEquals(emptyList(), viewModel.state.value.favorites)
        assertEquals(0, viewModel.state.value.favoritesCount)
    }

    @Test
    fun showsTenNewestFavoritesWithTotalCount() = runTest(mainDispatcherRule.dispatcher) {
        val ids = (1L..12L).map { TitleId(it, TitleType.MOVIE) }
        ids.forEach { favorites.setFavorite(it, isFavorite = true) }

        start(viewModel)

        assertEquals(ids.reversed().take(HOME_FAVORITES_LIMIT), viewModel.state.value.favorites.map { it.id })
        assertEquals(12, viewModel.state.value.favoritesCount)
    }

    @Test
    fun sectionIsTheSameForEveryCategory() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        start(viewModel)
        val before = viewModel.state.value.favorites

        viewModel.onIntent(HomeIntent.CategorySelected(TitleCategory.ANIME))

        assertEquals(before, viewModel.state.value.favorites)
        assertEquals(1, viewModel.state.value.favoritesCount)
    }

    @Test
    fun favoritesUpdateWhenOneIsAddedOrRemoved() = runTest(mainDispatcherRule.dispatcher) {
        start(viewModel)

        favorites.setFavorite(MOVIE, isFavorite = true)
        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })

        favorites.setFavorite(MOVIE, isFavorite = false)
        assertEquals(0, viewModel.state.value.favoritesCount)
    }

    @Test
    fun favoritesFollowLanguageChange() = runTest(mainDispatcherRule.dispatcher) {
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
    fun favoriteClickOpensFavorite() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(HomeIntent.FavoriteClicked(MOVIE))

        assertEquals(HomeEffect.OpenFavorite(MOVIE), viewModel.effects.first())
    }

    @Test
    fun seeAllOpensFavorites() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(HomeIntent.SeeAllFavoritesClicked)

        assertEquals(HomeEffect.OpenFavorites, viewModel.effects.first())
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
    }
}
