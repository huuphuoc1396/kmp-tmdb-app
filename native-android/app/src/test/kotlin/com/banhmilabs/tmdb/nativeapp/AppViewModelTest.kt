package com.banhmilabs.tmdb.nativeapp

import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadingUntilOnboardingStateIsRead() {
        val viewModel = AppViewModel(FakeSettingsRepository())

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

    private fun repositoryWithCompleted(completed: Boolean) = FakeSettingsRepository(onboardingCompleted = completed)
}
