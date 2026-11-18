package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.HomeCategory
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import com.banhmilabs.tmdb.service.title.domain.HomeRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Behavior every [HomeListsRepository] must have; extended by the real implementation's and the fake's tests. */
abstract class HomeListsRepositoryContract {
    /** A repository with nothing cached whose remote serves [CONTRACT_TITLES] for every list. */
    protected abstract fun TestScope.createRepository(): HomeListsRepository

    /** The next refresh of the repository last created fails with [error]. */
    protected abstract fun failNextRefresh(error: AppError)

    @Test
    fun observeIsNullBeforeRefresh() = runTest {
        assertNull(createRepository().observe(MOVIES, HomeRow.POPULAR, Language.EN_US).first())
    }

    @Test
    fun refreshStoresTitlesForObserver() = runTest {
        val repository = createRepository()

        assertEquals(AppResult.Success(Unit), repository.refresh(MOVIES, HomeRow.POPULAR, Language.EN_US))

        assertEquals(CONTRACT_TITLES, repository.observe(MOVIES, HomeRow.POPULAR, Language.EN_US).first())
    }

    @Test
    fun failedRefreshKeepsCachedTitles() = runTest {
        val repository = createRepository()
        repository.refresh(MOVIES, HomeRow.POPULAR, Language.EN_US)
        failNextRefresh(AppError.Network)

        val result = repository.refresh(MOVIES, HomeRow.POPULAR, Language.EN_US, force = true)

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertEquals(CONTRACT_TITLES, repository.observe(MOVIES, HomeRow.POPULAR, Language.EN_US).first())
    }

    @Test
    fun failedRefreshWithoutCacheStaysNull() = runTest {
        val repository = createRepository()
        failNextRefresh(AppError.Network)

        assertIs<AppResult.Failure>(repository.refresh(MOVIES, HomeRow.POPULAR, Language.EN_US))
        assertNull(repository.observe(MOVIES, HomeRow.POPULAR, Language.EN_US).first())
    }

    @Test
    fun languagesAreCachedSeparately() = runTest {
        val repository = createRepository()

        repository.refresh(MOVIES, HomeRow.POPULAR, Language.PT_BR)

        assertNull(repository.observe(MOVIES, HomeRow.POPULAR, Language.EN_US).first())
    }

    @Test
    fun listsAreCachedSeparately() = runTest {
        val repository = createRepository()

        repository.refresh(MOVIES, HomeRow.POPULAR, Language.EN_US)

        assertNull(repository.observe(MOVIES, HomeRow.FEATURED, Language.EN_US).first())
    }

    companion object {
        private val MOVIES = HomeCategory.MOVIES

        /** Two films, the second without a poster. */
        val CONTRACT_TITLES = listOf(
            Title(TitleId(550, TitleType.MOVIE), "Fight Club", "https://image.tmdb.org/t/p/w500/fight-club.jpg"),
            Title(TitleId(13, TitleType.MOVIE), "Forrest Gump", null),
        )
    }
}
