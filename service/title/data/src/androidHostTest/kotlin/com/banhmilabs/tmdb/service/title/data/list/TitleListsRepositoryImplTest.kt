package com.banhmilabs.tmdb.service.title.data.list

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleListsRepositoryContract
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class TitleListsRepositoryImplTest : TitleListsRepositoryContract() {
    private val env = TitleListTestEnvironment()

    override fun TestScope.createRepository(): TitleListsRepository = with(env) { createRepository() }

    override fun failNextRefresh(error: AppError) {
        env.nextError = error
    }

    @Test
    fun refreshSendsLanguageAndPage() = runTest {
        createRepository().refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.PT_BR)

        val url = env.requests.single().url
        assertEquals("/3/movie/popular", url.encodedPath)
        assertEquals("pt-BR", url.parameters["language"])
        assertEquals("1", url.parameters["page"])
    }

    @Test
    fun discoverRequestSendsFilters() = runTest {
        createRepository().refresh(TitleCategory.NOVELAS, TitleListType.POPULAR, Language.EN_US)

        val url = env.requests.single().url
        assertEquals("/3/discover/tv", url.encodedPath)
        assertEquals("BR|MX|CO|AR|VE|CL|PE", url.parameters["with_origin_country"])
        assertEquals("false", url.parameters["include_adult"])
    }

    @Test
    fun freshListIsNotRefetched() = runTest {
        val repository = createRepository()
        repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US)
        env.now += 1.hours

        assertEquals(
            AppResult.Success(Unit),
            repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US),
        )
        assertEquals(1, env.requests.size)
    }

    @Test
    fun staleListIsRefetched() = runTest {
        val repository = createRepository()
        repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US)
        env.now += 7.hours

        repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US)

        assertEquals(2, env.requests.size)
    }

    @Test
    fun forcedRefreshIgnoresAge() = runTest {
        val repository = createRepository()
        repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US)

        repository.refresh(TitleCategory.MOVIES, TitleListType.FEATURED, Language.EN_US, force = true)

        assertEquals(2, env.requests.size)
    }

    @Test
    fun concurrentRefreshesSendOneRequest() = runTest {
        val repository = createRepository()
        env.gate = CompletableDeferred()

        val first = async { repository.refresh(TitleCategory.ANIME, TitleListType.FEATURED, Language.EN_US) }
        env.requestStarted.await()
        val second = async {
            repository.refresh(
                TitleCategory.ANIME,
                TitleListType.FEATURED,
                Language.EN_US,
                force = true,
            )
        }
        testScheduler.advanceUntilIdle()
        env.gate?.complete(Unit)

        assertEquals(AppResult.Success(Unit), first.await())
        assertEquals(AppResult.Success(Unit), second.await())
        assertEquals(1, env.requests.size)
    }

    @Test
    fun networkErrorWithCacheKeepsTitles() = runTest {
        val repository = createRepository()
        repository.refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US)
        env.now += 7.hours
        failNextRefresh(AppError.Network)

        val result = repository.refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US)

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertEquals(
            CONTRACT_TITLES,
            repository.observe(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US).first(),
        )
    }

    @Test
    fun unauthorizedIsReported() = runTest {
        val repository = createRepository()
        failNextRefresh(AppError.Unauthorized)

        assertEquals(
            AppResult.Failure(AppError.Unauthorized),
            repository.refresh(TitleCategory.MOVIES, TitleListType.POPULAR, Language.EN_US),
        )
    }
}
