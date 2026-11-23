package com.banhmilabs.tmdb.cmp.feature.favorites.ui

import app.cash.turbine.test
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

class FavoritesViewModelTest : MainDispatcherTest() {
    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { FavoritesViewModel(ObserveFavoriteTitles(favorites, details)) }

    @Test
    fun listsAllFavoritesNewestFirst() = runTest(testDispatcher) {
        val ids = (1L..12L).map { TitleId(it, TitleType.MOVIE) }
        ids.forEach { favorites.setFavorite(it, isFavorite = true) }

        start()

        assertEquals(ids.reversed(), viewModel.state.value.favorites.map { it.id })
    }

    @Test
    fun emptyWithoutFavorites() = runTest(testDispatcher) {
        start()

        assertEquals(FavoritesUiState(), viewModel.state.value)
    }

    @Test
    fun updatesWhenFavoriteRemoved() = runTest(testDispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start()

        favorites.setFavorite(SERIES, isFavorite = false)

        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
    }

    @Test
    fun followsLanguageChange() = runTest(testDispatcher) {
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS.copy(name = "Clube da Luta"))
        details.refresh(MOVIE, Language.PT_BR)
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS)
        details.refresh(MOVIE, Language.EN_US)
        favorites.setFavorite(MOVIE, isFavorite = true)

        start(Language.PT_BR)
        assertEquals("Clube da Luta", viewModel.state.value.favorites.single().summary?.name)

        viewModel.onIntent(FavoritesIntent.LanguageChanged(Language.EN_US))
        assertEquals("Fight Club", viewModel.state.value.favorites.single().summary?.name)
    }

    @Test
    fun titleClickOpensTitle() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(FavoritesIntent.TitleClicked(MOVIE))

            assertEquals(FavoritesEffect.OpenTitle(MOVIE), awaitItem())
        }
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
