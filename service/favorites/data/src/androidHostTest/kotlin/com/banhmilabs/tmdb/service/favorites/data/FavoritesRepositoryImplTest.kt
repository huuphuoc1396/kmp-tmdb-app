package com.banhmilabs.tmdb.service.favorites.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.banhmilabs.tmdb.core.testing.TestDispatcherProvider
import com.banhmilabs.tmdb.service.favorites.data.generated.FavoritesDatabase
import com.banhmilabs.tmdb.service.favorites.data.local.FavoritesLocalDataSource
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.favorites.fake.FavoritesRepositoryContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class FavoritesRepositoryImplTest : FavoritesRepositoryContract() {
    private val database = FavoritesDatabase(
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FavoritesDatabase.Schema.create(it) },
    )
    private var now: Instant = Instant.parse("2026-11-21T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now.also { now += 1.milliseconds }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun TestScope.createRepository(): FavoritesRepository {
        val dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler))
        return FavoritesRepositoryImpl(local = FavoritesLocalDataSource(lazyOf(database), dispatchers), clock = clock)
    }

    @Test
    fun addedAtIsKeptWhenSetAgain() = runTest {
        val repository = createRepository()
        val firstAdded = now
        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)

        repository.setFavorite(CONTRACT_MOVIE_ID, isFavorite = true)

        val stored = database.favoriteQueries
            .selectAddedAt(tmdbId = CONTRACT_MOVIE_ID.tmdbId, type = CONTRACT_MOVIE_ID.type.name)
            .executeAsOne()
        assertEquals(firstAdded.toEpochMilliseconds(), stored)
    }
}
