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
    fun startsInContent() {
        assertEquals(OnboardingUiState.Content, viewModel.uiState.value)
    }

    @Test
    fun getStartedCompletesOnboardingThenFinishes() = runTest(testDispatcher) {
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Content, awaitItem())
            viewModel.onGetStartedClick()
            assertEquals(OnboardingUiState.Navigating, awaitItem())
            assertEquals(OnboardingUiState.Finished, awaitItem())
        }
        assertTrue(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun getStartedFinishesWhenSaveFails() = runTest(testDispatcher) {
        repository.failNextComplete(AppError.Unknown)

        viewModel.onGetStartedClick()
        testScheduler.advanceUntilIdle()

        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
        assertFalse(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun repeatedTapsFinishOnce() = runTest(testDispatcher) {
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Content, awaitItem())
            viewModel.onGetStartedClick()
            viewModel.onGetStartedClick()
            assertEquals(OnboardingUiState.Navigating, awaitItem())
            assertEquals(OnboardingUiState.Finished, awaitItem())
            viewModel.onGetStartedClick()
            expectNoEvents()
        }
    }
}
