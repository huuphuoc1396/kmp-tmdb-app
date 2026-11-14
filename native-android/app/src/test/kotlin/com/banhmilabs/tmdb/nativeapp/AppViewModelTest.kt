package com.banhmilabs.tmdb.nativeapp

import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadingUntilOnboardingStateIsRead() {
        val repository = mockk<SettingsRepository> {
            every { observeOnboardingCompleted() } returns flow { awaitCancellation() }
        }
        val viewModel = AppViewModel(repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartDestination.Loading, viewModel.startDestination.value)
    }

    @Test
    fun startsOnOnboardingWhenNotCompleted() {
        val viewModel = AppViewModel(repositoryWithCompleted(false))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartDestination.Onboarding, viewModel.startDestination.value)
    }

    @Test
    fun startsOnHomeWhenCompleted() {
        val viewModel = AppViewModel(repositoryWithCompleted(true))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartDestination.Home, viewModel.startDestination.value)
    }

    private fun repositoryWithCompleted(completed: Boolean) = mockk<SettingsRepository> {
        every { observeOnboardingCompleted() } returns flowOf(completed)
    }
}
