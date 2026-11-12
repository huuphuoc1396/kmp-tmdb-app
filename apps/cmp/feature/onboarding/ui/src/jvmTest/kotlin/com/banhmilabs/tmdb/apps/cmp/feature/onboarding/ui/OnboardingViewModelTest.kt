package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import app.cash.turbine.test
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingViewModelTest : MainDispatcherTest(StandardTestDispatcher()) {
    private val repository = FakeSettingsRepository()
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun startsNotNavigating() {
        assertEquals(OnboardingUiState(isNavigating = false), viewModel.state.value)
    }

    @Test
    fun getStartedCompletesOnboardingThenNavigatesHome() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(OnboardingIntent.GetStartedClicked)

            assertTrue(viewModel.state.value.isNavigating)
            assertEquals(OnboardingEffect.NavigateToHome, awaitItem())
        }
        assertTrue(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun getStartedNavigatesHomeWhenSaveFails() = runTest(testDispatcher) {
        repository.failNextComplete(AppError.Unknown)

        viewModel.effects.test {
            viewModel.onIntent(OnboardingIntent.GetStartedClicked)

            assertEquals(OnboardingEffect.NavigateToHome, awaitItem())
        }
        assertFalse(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun repeatedTapsNavigateOnce() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onIntent(OnboardingIntent.GetStartedClicked)
            viewModel.onIntent(OnboardingIntent.GetStartedClicked)

            assertEquals(OnboardingEffect.NavigateToHome, awaitItem())
            viewModel.onIntent(OnboardingIntent.GetStartedClicked)
            testScheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }
}
