package com.banhmilabs.tmdb.service.settings.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeSettingsRepositoryTest : SettingsRepositoryContract() {
    override fun TestScope.createRepository(): SettingsRepository = FakeSettingsRepository()

    @Test
    fun failNextCompleteReturnsErrorAndKeepsState() = runTest {
        val repository = FakeSettingsRepository()
        repository.failNextComplete(AppError.Unknown)

        assertEquals(AppResult.Failure(AppError.Unknown), repository.completeOnboarding())
        assertFalse(repository.observeOnboardingCompleted().first())
        assertEquals(AppResult.Success(Unit), repository.completeOnboarding())
    }

    @Test
    fun startsCompletedWhenConstructedSo() = runTest {
        assertTrue(FakeSettingsRepository(onboardingCompleted = true).observeOnboardingCompleted().first())
    }
}
