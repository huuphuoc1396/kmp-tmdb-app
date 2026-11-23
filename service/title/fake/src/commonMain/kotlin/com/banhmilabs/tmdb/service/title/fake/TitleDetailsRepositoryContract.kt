package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.CastMember
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.Trailer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

abstract class TitleDetailsRepositoryContract {
    protected abstract fun TestScope.createRepository(): TitleDetailsRepository

    protected abstract fun failNextRefresh(error: AppError)

    @Test
    fun observeIsNullBeforeRefresh() = runTest {
        assertNull(createRepository().observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }

    @Test
    fun refreshStoresDetailsForObserver() = runTest {
        val repository = createRepository()

        assertEquals(AppResult.Success(Unit), repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US))

        assertEquals(CONTRACT_MOVIE_DETAILS, repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }

    @Test
    fun failedRefreshKeepsCachedDetails() = runTest {
        val repository = createRepository()
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)
        failNextRefresh(AppError.Network)

        val result = repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true)

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertEquals(CONTRACT_MOVIE_DETAILS, repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }

    @Test
    fun failedRefreshWithoutCacheStaysNull() = runTest {
        val repository = createRepository()
        failNextRefresh(AppError.Network)

        assertIs<AppResult.Failure>(repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US))
        assertNull(repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }

    @Test
    fun languagesAreCachedSeparately() = runTest {
        val repository = createRepository()

        repository.refresh(CONTRACT_MOVIE_ID, Language.PT_BR)

        assertNull(repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }

    @Test
    fun titlesAreCachedSeparately() = runTest {
        val repository = createRepository()

        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)

        assertNull(repository.observe(TitleId(CONTRACT_MOVIE_ID.tmdbId, TitleType.TV), Language.EN_US).first())
    }

    companion object {
        val CONTRACT_MOVIE_ID = TitleId(550, TitleType.MOVIE)

        val CONTRACT_MOVIE_DETAILS = TitleDetails(
            id = CONTRACT_MOVIE_ID,
            name = "Fight Club",
            releaseDate = LocalDate(1999, 10, 15),
            runtimeMinutes = 139,
            ratingPercent = 84,
            synopsis = "An insomniac office worker and a soap maker form an underground fight club.",
            artworkUrl = "https://image.tmdb.org/t/p/w500/fight-club.jpg",
            genres = listOf("Drama", "Thriller"),
            cast = listOf(
                CastMember("Edward Norton", "https://image.tmdb.org/t/p/w185/norton.jpg"),
                CastMember("Brad Pitt", null),
            ),
            trailer = Trailer("BdJKm16Co6M"),
            recommendations = listOf(
                Title(TitleId(680, TitleType.MOVIE), "Pulp Fiction", "https://image.tmdb.org/t/p/w500/pulp.jpg"),
                Title(TitleId(1399, TitleType.TV), "Game of Thrones", null),
            ),
        )
    }
}
