package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleDetailsRepositoryContract
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class TitleDetailsRepositoryImplTest : TitleDetailsRepositoryContract() {
    private val env = TitleDetailsTestEnvironment()

    override fun TestScope.createRepository(): TitleDetailsRepository = with(env) { createRepository() }

    override fun failNextRefresh(error: AppError) {
        env.nextError = error
    }

    @Test
    fun movieRequestSendsPathAndParameters() = runTest {
        createRepository().refresh(CONTRACT_MOVIE_ID, Language.PT_BR)

        val url = env.requests.single().url
        assertEquals("/3/movie/550", url.encodedPath)
        assertEquals("pt-BR", url.parameters["language"])
        assertEquals("credits,videos,recommendations", url.parameters["append_to_response"])
        assertEquals("pt,en", url.parameters["include_video_language"])
    }

    @Test
    fun englishRequestAsksForEnglishVideos() = runTest {
        createRepository().refresh(CONTRACT_MOVIE_ID, Language.EN_US)

        val url = env.requests.single().url
        assertEquals("en-US", url.parameters["language"])
        assertEquals("en,en", url.parameters["include_video_language"])
    }

    @Test
    fun freshDetailsAreNotRefetched() = runTest {
        val repository = createRepository()
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)
        env.now += 23.hours

        assertEquals(AppResult.Success(Unit), repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US))
        assertEquals(1, env.requests.size)
    }

    @Test
    fun staleDetailsAreRefetched() = runTest {
        val repository = createRepository()
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)
        env.now += 25.hours

        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)

        assertEquals(2, env.requests.size)
    }

    @Test
    fun forcedRefreshIgnoresAge() = runTest {
        val repository = createRepository()
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)

        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true)

        assertEquals(2, env.requests.size)
    }

    @Test
    fun concurrentRefreshesSendOneRequest() = runTest {
        val repository = createRepository()
        env.gate = CompletableDeferred()

        val first = async { repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US) }
        env.requestStarted.await()
        val second = async { repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true) }
        testScheduler.advanceUntilIdle()
        env.gate?.complete(Unit)

        assertEquals(AppResult.Success(Unit), first.await())
        assertEquals(AppResult.Success(Unit), second.await())
        assertEquals(1, env.requests.size)
    }

    @Test
    fun notFoundKeepsCacheAndIsReported() = runTest {
        val repository = createRepository()
        repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US)
        failNextRefresh(AppError.NotFound)

        val result = repository.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true)

        assertEquals(AppResult.Failure(AppError.NotFound), result)
        assertEquals(CONTRACT_MOVIE_DETAILS, repository.observe(CONTRACT_MOVIE_ID, Language.EN_US).first())
    }
}
