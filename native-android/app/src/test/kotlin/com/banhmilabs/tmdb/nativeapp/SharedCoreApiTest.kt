package com.banhmilabs.tmdb.nativeapp

import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCoreApiTest {
    @Test
    fun readsOnboardingCompletedThroughPublishedDomainApi() = runTest {
        val repository = mockk<SettingsRepository> {
            every { observeOnboardingCompleted() } returns flowOf(true)
        }

        assertTrue(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun readsNotCompletedThroughPublishedDomainApi() = runTest {
        val repository = mockk<SettingsRepository> {
            every { observeOnboardingCompleted() } returns flowOf(false)
        }

        assertFalse(repository.observeOnboardingCompleted().first())
    }
}
