package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.HomeCategory
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import com.banhmilabs.tmdb.service.title.domain.HomeRow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeHomeListsRepositoryTest : HomeListsRepositoryContract() {
    private lateinit var repository: FakeHomeListsRepository

    override fun TestScope.createRepository(): HomeListsRepository =
        FakeHomeListsRepository(CONTRACT_TITLES).also { repository = it }

    override fun failNextRefresh(error: AppError) {
        repository.failNextRefresh(error)
    }

    @Test
    fun failNextRefreshFailsOnlyOnce() = runTest {
        val fake = FakeHomeListsRepository(CONTRACT_TITLES)
        fake.failNextRefresh(AppError.Unknown)

        assertEquals(
            AppResult.Failure(AppError.Unknown),
            fake.refresh(HomeCategory.SERIES, HomeRow.FEATURED, Language.EN_US),
        )
        assertEquals(AppResult.Success(Unit), fake.refresh(HomeCategory.SERIES, HomeRow.FEATURED, Language.EN_US))
    }
}
