package com.banhmilabs.tmdb.service.title.domain

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveFavoriteTitlesTest {
    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository(
        mapOf(MOVIE to CONTRACT_MOVIE_DETAILS, SERIES to SERIES_DETAILS),
    )
    private val observeFavoriteTitles = ObserveFavoriteTitles(favorites, details)

    @Test
    fun emitsEmptyListWithoutFavorites() = runTest {
        assertEquals(emptyList(), observeFavoriteTitles(Language.EN_US).first())
    }

    @Test
    fun listsFavoritesNewestFirst() = runTest {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)

        assertEquals(listOf(SERIES, MOVIE), observeFavoriteTitles(Language.EN_US).first().map { it.id })
    }

    @Test
    fun usesCurrentLanguageSummary() = runTest {
        details.refresh(MOVIE, Language.EN_US)
        favorites.setFavorite(MOVIE, isFavorite = true)

        val summary = observeFavoriteTitles(Language.EN_US).first().single().summary

        assertEquals(
            TitleSummary(MOVIE, "Fight Club", CONTRACT_MOVIE_DETAILS.artworkUrl, 1999, "Drama"),
            summary,
        )
    }

    @Test
    fun fallsBackToOtherLanguageThenReplacesAfterRefresh() = runTest {
        details.refresh(MOVIE, Language.PT_BR)
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS.copy(name = "Fight Club (en)"))
        details.failNextRefresh(AppError.Network)
        favorites.setFavorite(MOVIE, isFavorite = true)

        observeFavoriteTitles(Language.EN_US).test {
            assertEquals("Fight Club", awaitItem().single().summary?.name)
            details.refresh(MOVIE, Language.EN_US)
            assertEquals("Fight Club (en)", awaitItem().single().summary?.name)
        }
    }

    @Test
    fun keepsFavoriteWithNullSummaryWhenNoData() = runTest {
        favorites.setFavorite(UNKNOWN, isFavorite = true)

        observeFavoriteTitles(Language.EN_US).test {
            assertEquals(listOf(FavoriteTitle(UNKNOWN, null)), awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun keepsFallbackWhenRefreshFails() = runTest {
        details.refresh(MOVIE, Language.PT_BR)
        details.failNextRefresh(AppError.NotFound)
        favorites.setFavorite(MOVIE, isFavorite = true)

        observeFavoriteTitles(Language.EN_US).test {
            assertEquals("Fight Club", awaitItem().single().summary?.name)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun refreshesOnlyTitlesMissingCurrentLanguage() = runTest {
        details.refresh(MOVIE, Language.EN_US)
        val before = details.refreshRequests.size
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)

        observeFavoriteTitles(Language.EN_US).test {
            awaitItem()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            listOf(FakeTitleDetailsRepository.RefreshRequest(SERIES, Language.EN_US, force = false)),
            details.refreshRequests.drop(before),
        )
    }

    @Test
    fun removedFavoriteLeavesList() = runTest {
        favorites.setFavorite(MOVIE, isFavorite = true)
        favorites.setFavorite(SERIES, isFavorite = true)

        observeFavoriteTitles(Language.EN_US).test {
            assertEquals(listOf(SERIES, MOVIE), awaitItem().map { it.id })
            favorites.setFavorite(SERIES, isFavorite = false)
            assertEquals(listOf(MOVIE), awaitItem().map { it.id })
        }
    }

    @Test
    fun sameTitleAppearsOnce() = runTest {
        details.refresh(MOVIE, Language.EN_US)
        details.refresh(MOVIE, Language.PT_BR)
        favorites.setFavorite(MOVIE, isFavorite = true)

        assertEquals(listOf(MOVIE), observeFavoriteTitles(Language.PT_BR).first().map { it.id })
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
        val SERIES = TitleId(1399, TitleType.TV)
        val UNKNOWN = TitleId(1, TitleType.MOVIE)
        val SERIES_DETAILS = CONTRACT_MOVIE_DETAILS.copy(id = SERIES, name = "Game of Thrones")
    }
}
