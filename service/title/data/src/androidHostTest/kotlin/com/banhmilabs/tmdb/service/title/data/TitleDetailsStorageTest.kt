package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.CastMember
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract.Companion.CONTRACT_MOVIE_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** What a refresh stores: series, replaced rows, and nothing for an invalid response. */
class TitleDetailsStorageTest {
    private val env = TitleDetailsTestEnvironment()

    @Test
    fun seriesIsStoredAndObserved() = runTest {
        env.responseBody = env.fixture("tv_details.json")
        val repository = with(env) { createRepository() }
        val id = TitleId(1399, TitleType.TV)

        repository.refresh(id, Language.EN_US)

        assertEquals("/3/tv/1399", env.requests.single().url.encodedPath)
        assertEquals(
            TitleDetails(
                id = id,
                name = "Game of Thrones",
                releaseDate = LocalDate(2011, 4, 17),
                runtimeMinutes = 80,
                ratingPercent = 85,
                synopsis = null,
                artworkUrl = "https://image.tmdb.org/t/p/w780/got-backdrop.jpg",
                genres = listOf("Sci-Fi & Fantasy"),
                cast = listOf(CastMember("Peter Dinklage", "https://image.tmdb.org/t/p/w185/dinklage.jpg")),
                trailer = null,
                recommendations = listOf(
                    Title(TitleId(1402, TitleType.TV), "The Walking Dead", "https://image.tmdb.org/t/p/w500/twd.jpg"),
                ),
            ),
            repository.observe(id, Language.EN_US).first(),
        )
    }

    @Test
    fun refreshReplacesChildRows() = runTest {
        val repository = with(env) { createRepository() }
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)
        env.responseBody = env.fixture("movie_details.json")
            .replace("""{ "id": 287, "name": "Brad Pitt", "profile_path": null, "order": 1 },""", "")
            .replace("""{ "id": 53, "name": "Thriller" }""", """{ "id": 80, "name": "Crime" }""")

        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true)

        val details = repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first()
        assertEquals(listOf("Edward Norton"), details?.cast?.map { it.name })
        assertEquals(listOf("Drama", "Crime"), details?.genres)
    }

    @Test
    fun responseWithoutNameIsUnknownAndNotStored() = runTest {
        env.responseBody = """{"id":550,"overview":"No name"}"""
        val repository = with(env) { createRepository() }

        assertEquals(AppResult.Failure(AppError.Unknown), repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US))
        assertEquals(null, repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }
}
