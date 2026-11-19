package com.banhmilabs.tmdb.service.title.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTitleListsRepositoryTest : TitleListsRepositoryContract() {
    private lateinit var repository: FakeTitleListsRepository

    override fun TestScope.createRepository(): TitleListsRepository =
        FakeTitleListsRepository(CONTRACT_TITLES).also { repository = it }

    override fun failNextRefresh(error: AppError) {
        repository.failNextRefresh(error)
    }

    @Test
    fun failNextRefreshFailsOnlyOnce() = runTest {
        val fake = FakeTitleListsRepository(CONTRACT_TITLES)
        fake.failNextRefresh(AppError.Unknown)

        assertEquals(
            AppResult.Failure(AppError.Unknown),
            fake.refresh(TitleCategory.SERIES, TitleListType.FEATURED, Language.EN_US),
        )
        assertEquals(
            AppResult.Success(Unit),
            fake.refresh(TitleCategory.SERIES, TitleListType.FEATURED, Language.EN_US),
        )
    }

    @Test
    fun setRemoteTitlesAppliesOnlyToThatList() = runTest {
        val fake = FakeTitleListsRepository(CONTRACT_TITLES)
        val anime = CONTRACT_TITLES.take(1)
        fake.setRemoteTitles(TitleCategory.ANIME, TitleListType.POPULAR, anime)

        fake.refresh(TitleCategory.ANIME, TitleListType.POPULAR, Language.PT_BR)
        fake.refresh(TitleCategory.ANIME, TitleListType.FEATURED, Language.PT_BR)

        assertEquals(anime, fake.observe(TitleCategory.ANIME, TitleListType.POPULAR, Language.PT_BR).first())
        assertEquals(
            CONTRACT_TITLES,
            fake.observe(TitleCategory.ANIME, TitleListType.FEATURED, Language.PT_BR).first(),
        )
    }
}
