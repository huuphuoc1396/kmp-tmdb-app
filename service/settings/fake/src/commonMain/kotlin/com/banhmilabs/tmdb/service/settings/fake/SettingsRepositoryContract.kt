package com.banhmilabs.tmdb.service.settings.fake

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class SettingsRepositoryContract {
    protected abstract fun TestScope.createRepository(): SettingsRepository

    @Test
    fun onboardingNotCompletedInitially() = runTest {
        assertFalse(createRepository().observeOnboardingCompleted().first())
    }

    @Test
    fun completeOnboardingReturnsSuccess() = runTest {
        assertEquals(AppResult.Success(Unit), createRepository().completeOnboarding())
    }

    @Test
    fun completeOnboardingNotifiesObserver() = runTest {
        val repository = createRepository()

        repository.observeOnboardingCompleted().test {
            assertFalse(awaitItem())
            repository.completeOnboarding()
            assertTrue(awaitItem())
        }
    }

    @Test
    fun completeOnboardingTwiceStaysCompleted() = runTest {
        val repository = createRepository()

        repository.completeOnboarding()

        assertEquals(AppResult.Success(Unit), repository.completeOnboarding())
        assertTrue(repository.observeOnboardingCompleted().first())
    }
}
