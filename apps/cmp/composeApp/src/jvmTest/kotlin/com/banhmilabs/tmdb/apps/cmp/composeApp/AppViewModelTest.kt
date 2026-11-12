package com.banhmilabs.tmdb.apps.cmp.composeApp

import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest : MainDispatcherTest() {
    @Test
    fun startsOnOnboardingWhenNotCompleted() {
        val viewModel = AppViewModel(FakeSettingsRepository(onboardingCompleted = false))

        assertEquals(StartDestination.Onboarding, viewModel.startDestination.value)
    }

    @Test
    fun startsOnHomeWhenCompleted() {
        val viewModel = AppViewModel(FakeSettingsRepository(onboardingCompleted = true))

        assertEquals(StartDestination.Home, viewModel.startDestination.value)
    }
}
