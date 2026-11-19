package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** How TMDB results become cached [Title]s. */
class TitleListMappingTest {
    private val env = TitleTestEnvironment()

    @Test
    fun tvListStoresTvTitles() = runTest {
        env.responseBody = env.fixture("tv_list.json")

        assertEquals(
            listOf(TitleId(1399, TitleType.TV), TitleId(94997, TitleType.TV)),
            refreshed(TitleCategory.SERIES).map { it.id },
        )
    }

    @Test
    fun posterPathBecomesW500Url() = runTest {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/aFRDH3P7TX61FVGpaLhKr6QiOC1.jpg",
            refreshedMovieFixture().first().posterUrl,
        )
    }

    @Test
    fun titleWithoutPosterHasNullUrl() = runTest {
        assertNull(refreshedMovieFixture()[1].posterUrl)
    }

    @Test
    fun entryWithoutNameIsDropped() = runTest {
        assertEquals(
            listOf("Demon Slayer: Infinity Castle", "War of the Worlds", "Superman"),
            refreshedMovieFixture().map { it.name },
        )
    }

    @Test
    fun refreshReplacesPreviousTitlesInOrder() = runTest {
        val repository = with(env) { createRepository() }
        repository.refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US)
        env.responseBody = env.fixture("movie_list.json")

        repository.refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US, force = true)

        assertEquals(
            listOf(1311031L, 755898L, 1061474L),
            repository.observe(
                TitleCategory.MOVIES,
                TitleListType.POPULAR,
                Language.EN_US,
            ).first()?.map { it.id.tmdbId },
        )
    }

    private suspend fun TestScope.refreshedMovieFixture(): List<Title> {
        env.responseBody = env.fixture("movie_list.json")
        return refreshed(TitleCategory.MOVIES)
    }

    private suspend fun TestScope.refreshed(category: TitleCategory): List<Title> {
        val repository = with(env) { createRepository() }
        repository.refresh(category, TitleListType.POPULAR, Language.EN_US)
        return checkNotNull(repository.observe(category, TitleListType.POPULAR, Language.EN_US).first())
    }
}
