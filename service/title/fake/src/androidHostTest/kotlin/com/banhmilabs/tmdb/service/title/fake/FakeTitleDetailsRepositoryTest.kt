package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTitleDetailsRepositoryTest : TitleDetailsRepositoryContract() {
    private lateinit var repository: FakeTitleDetailsRepository

    override fun TestScope.createRepository(): TitleDetailsRepository =
        FakeTitleDetailsRepository(mapOf(CONTRACT_MOVIE_ID to CONTRACT_MOVIE_DETAILS)).also { repository = it }

    override fun failNextRefresh(error: AppError) {
        repository.failNextRefresh(error)
    }

    @Test
    fun refreshWithoutRemoteFailsNotFound() = runTest {
        assertEquals(
            AppResult.Failure(AppError.NotFound),
            FakeTitleDetailsRepository().refresh(CONTRACT_MOVIE_ID, Language.EN_US),
        )
    }

    @Test
    fun setRemoteDetailsAppliesToThatTitle() = runTest {
        val fake = FakeTitleDetailsRepository()
        val series = CONTRACT_MOVIE_DETAILS.copy(id = TitleId(1399, TitleType.TV), name = "Game of Thrones")
        fake.setRemoteDetails(series.id, series)

        fake.refresh(series.id, Language.PT_BR)

        assertEquals(series, fake.observe(series.id, Language.PT_BR).first())
    }

    @Test
    fun refreshRequestsAreRecorded() = runTest {
        val fake = FakeTitleDetailsRepository()
        fake.failNextRefresh(AppError.Network)

        fake.refresh(CONTRACT_MOVIE_ID, Language.EN_US, force = true)
        fake.refresh(CONTRACT_MOVIE_ID, Language.PT_BR)

        assertEquals(
            listOf(
                FakeTitleDetailsRepository.RefreshRequest(CONTRACT_MOVIE_ID, Language.EN_US, force = true),
                FakeTitleDetailsRepository.RefreshRequest(CONTRACT_MOVIE_ID, Language.PT_BR, force = false),
            ),
            fake.refreshRequests,
        )
    }
}
