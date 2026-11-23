package com.banhmilabs.tmdb.service.title.data.details.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.testing.TestDispatcherProvider
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsEntry
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsKey
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.TitleSummary
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class TitleDetailsLocalDataSourceTest {
    private val io = newSingleThreadContext(IO_THREAD)
    private val dispatchers = TestDispatcherProvider(io)
    private var openedOn: String? = null
    private val database = lazy {
        openedOn = Thread.currentThread().name.substringBefore(" @")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TitleDatabase.Schema.create(driver)
        TitleDatabase(driver)
    }

    @AfterTest
    fun closeIo() {
        io.close()
    }

    @Test
    fun databaseOpensOnFirstQueryOnIo() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        val cached = local.observe(KEY)
        assertNull(openedOn)

        cached.first()
        assertEquals(IO_THREAD, openedOn)
    }

    @Test
    fun summaryIsNullWhenNotStored() = runBlocking {
        assertNull(TitleDetailsLocalDataSource(database, dispatchers).observeSummary(KEY).first())
    }

    @Test
    fun summaryHasNameYearPosterAndFirstGenre() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        local.upsert(KEY, ENTRY, FETCHED_AT)

        assertEquals(
            TitleSummary(
                id = KEY.id,
                name = "Fight Club",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                releaseYear = 1999,
                firstGenre = "Drama",
            ),
            local.observeSummary(KEY).first(),
        )
    }

    @Test
    fun summaryHasNoPosterWhenArtworkIsBackdrop() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        local.upsert(KEY, ENTRY.copy(artworkPath = "/backdrop.jpg", artworkIsPoster = false), FETCHED_AT)

        assertNull(local.observeSummary(KEY).first()?.posterUrl)
    }

    @Test
    fun summaryHasNoGenreOrYearWhenNoneStored() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        local.upsert(KEY, ENTRY.copy(genres = emptyList(), releaseDate = null), FETCHED_AT)

        val summary = local.observeSummary(KEY).first()
        assertNull(summary?.firstGenre)
        assertNull(summary?.releaseYear)
    }

    @Test
    fun summaryIsPerLanguage() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        local.upsert(KEY, ENTRY, FETCHED_AT)

        assertNull(local.observeSummary(KEY.copy(language = Language.PT_BR)).first())
    }

    @Test
    fun summaryUpdatesAfterUpsert() = runBlocking {
        val local = TitleDetailsLocalDataSource(database, dispatchers)

        local.observeSummary(KEY).test {
            assertNull(awaitItem())
            local.upsert(KEY, ENTRY, FETCHED_AT)
            assertEquals("Drama", awaitItem()?.firstGenre)
            local.upsert(KEY, ENTRY.copy(genres = listOf("Thriller")), FETCHED_AT)
            assertEquals("Thriller", awaitItem()?.firstGenre)
        }
    }

    private companion object {
        const val IO_THREAD = "io"
        val KEY = TitleDetailsKey(TitleId(550, TitleType.MOVIE), Language.EN_US)
        val FETCHED_AT = Instant.parse("2026-11-23T10:00:00Z")
        val ENTRY = TitleDetailsEntry(
            name = "Fight Club",
            releaseDate = LocalDate(1999, 10, 15),
            runtimeMinutes = 139,
            ratingPercent = 84,
            synopsis = null,
            artworkPath = "/poster.jpg",
            artworkIsPoster = true,
            trailerKey = null,
            genres = listOf("Drama", "Thriller"),
            cast = emptyList(),
            recommendations = emptyList(),
        )
    }
}
