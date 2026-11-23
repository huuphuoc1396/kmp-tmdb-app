package com.banhmilabs.tmdb.nativeapp.favorites

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.ObserveFavoriteTitles
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository()
    private val viewModel by lazy { FavoritesViewModel(ObserveFavoriteTitles(favorites, details), favorites) }

    @Test
    fun listsAllFavoritesNewestFirst() = runTest(mainDispatcherRule.dispatcher) {
        val ids = (1L..12L).map { TitleId(it, TitleType.MOVIE) }
        ids.forEach { favorites.setFavorite(it, isFavorite = true) }

        start()

        assertEquals(ids.reversed(), viewModel.state.value.favorites.map { it.id })
    }

    @Test
    fun emptyWithoutFavorites() = runTest(mainDispatcherRule.dispatcher) {
        start()

        assertEquals(FavoritesUiState(), viewModel.state.value)
    }

    @Test
    fun updatesWhenFavoriteRemoved() = runTest(mainDispatcherRule.dispatcher) {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)
        start()

        favorites.setFavorite(SERIES, isFavorite = false)

        assertEquals(listOf(MOVIE), viewModel.state.value.favorites.map { it.id })
    }

    @Test
    fun followsLanguageChange() = runTest(mainDispatcherRule.dispatcher) {
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
    fun titleClickOpensTitle() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(FavoritesIntent.TitleClicked(MOVIE))

        assertEquals(FavoritesEffect.OpenTitle(MOVIE), viewModel.effects.first())
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
