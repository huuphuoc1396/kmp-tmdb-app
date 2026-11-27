package com.banhmilabs.tmdb.service.title.domain

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.favorites.fake.FakeFavoritesRepository
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_DETAILS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveFavoriteTitlesVietnameseTest {
    private val favorites = FakeFavoritesRepository()
    private val details = FakeTitleDetailsRepository(mapOf(MOVIE to CONTRACT_MOVIE_DETAILS))
    private val observeFavoriteTitles = ObserveFavoriteTitles(favorites, details)

    @Test
    fun vietnameseFallsBackToEnglishFirst() = runTest {
        details.refresh(MOVIE, Language.PT_BR)
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS.copy(name = "Fight Club (en)"))
        details.refresh(MOVIE, Language.EN_US)
        details.failNextRefresh(AppError.Network)
        favorites.setFavorite(MOVIE, isFavorite = true)

        assertEquals("Fight Club (en)", observeFavoriteTitles(Language.VI_VN).first().single().summary?.name)
    }

    @Test
    fun vietnameseFallsBackToPortugueseWithoutEnglish() = runTest {
        details.refresh(MOVIE, Language.PT_BR)
        details.failNextRefresh(AppError.Network)
        favorites.setFavorite(MOVIE, isFavorite = true)

        assertEquals("Fight Club", observeFavoriteTitles(Language.VI_VN).first().single().summary?.name)
    }

    @Test
    fun vietnameseReplacesFallbackAfterRefresh() = runTest {
        details.refresh(MOVIE, Language.EN_US)
        details.setRemoteDetails(MOVIE, CONTRACT_MOVIE_DETAILS.copy(name = "Câu lạc bộ đấu sĩ"))
        details.failNextRefresh(AppError.Network)
        favorites.setFavorite(MOVIE, isFavorite = true)

        observeFavoriteTitles(Language.VI_VN).test {
            assertEquals("Fight Club", awaitItem().single().summary?.name)
            details.refresh(MOVIE, Language.VI_VN)
            assertEquals("Câu lạc bộ đấu sĩ", awaitItem().single().summary?.name)
        }
    }

    private companion object {
        val MOVIE = CONTRACT_MOVIE_DETAILS.id
    }
}
