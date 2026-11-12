package com.banhmilabs.tmdb.apps.cmp.composeApp

import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest : MainDispatcherTest(StandardTestDispatcher()) {
    @Test
    fun loadingUntilOnboardingStateIsRead() {
        val viewModel = AppViewModel(FakeSettingsRepository())

        assertEquals(StartDestination.Loading, viewModel.startDestination.value)
    }

    @Test
    fun startsOnOnboardingWhenNotCompleted() {
        val viewModel = AppViewModel(FakeSettingsRepository(onboardingCompleted = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartDestination.Onboarding, viewModel.startDestination.value)
    }

    @Test
    fun startsOnHomeWhenCompleted() {
        val viewModel = AppViewModel(FakeSettingsRepository(onboardingCompleted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartDestination.Home, viewModel.startDestination.value)
    }
}
