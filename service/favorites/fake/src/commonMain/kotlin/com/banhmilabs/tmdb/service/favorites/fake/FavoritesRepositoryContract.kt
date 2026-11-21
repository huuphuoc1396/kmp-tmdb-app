package com.banhmilabs.tmdb.service.favorites.fake

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Behavior every [FavoritesRepository] must have; extended by the real implementation's and the fake's tests. */
abstract class FavoritesRepositoryContract {
    /** A repository with no favorites. */
    protected abstract fun TestScope.createRepository(): FavoritesRepository

    @Test
    fun notFavoriteByDefault() = runTest {
        assertFalse(createRepository().observeIsFavorite(CONTRACT_MOVIE_ID).first())
    }

    @Test
    fun setFavoriteTrueMakesItFavorite() = runTest {
        val repository = createRepository()

        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)

        assertTrue(repository.observeIsFavorite(CONTRACT_MOVIE_ID).first())
    }

    @Test
    fun setFavoriteFalseRemovesIt() = runTest {
        val repository = createRepository()
        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)

        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = false)

        assertFalse(repository.observeIsFavorite(CONTRACT_MOVIE_ID).first())
    }

    @Test
    fun favoritesAreKeptPerTitle() = runTest {
        val repository = createRepository()

        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)

        assertFalse(repository.observeIsFavorite(OTHER_MOVIE_ID).first())
        assertFalse(repository.observeIsFavorite(TitleId(CONTRACT_MOVIE_ID.tmdbId, TitleType.TV)).first())
    }

    @Test
    fun settingTwiceIsIdempotent() = runTest {
        val repository = createRepository()

        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)
        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)
        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = false)

        assertFalse(repository.observeIsFavorite(CONTRACT_MOVIE_ID).first())
    }

    @Test
    fun observerSeesChanges() = runTest {
        val repository = createRepository()

        repository.observeIsFavorite(CONTRACT_MOVIE_ID).test {
            assertFalse(awaitItem())
            repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)
            assertTrue(awaitItem())
            repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = false)
            assertFalse(awaitItem())
        }
    }

    companion object {
        val CONTRACT_MOVIE_ID = TitleId(550, TitleType.MOVIE)
        private val OTHER_MOVIE_ID = TitleId(551, TitleType.MOVIE)
    }
}
