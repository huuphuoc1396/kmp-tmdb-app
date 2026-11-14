package com.banhmilabs.tmdb.nativeapp.onboarding

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<SettingsRepository> {
        coEvery { completeOnboarding() } returns AppResult.Success(Unit)
    }
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun startsNotNavigating() {
        assertEquals(OnboardingUiState(isNavigating = false), viewModel.state.value)
    }

    @Test
    fun getStartedCompletesOnboardingThenNavigatesHome() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onIntent(OnboardingIntent.GetStartedClicked)

        assertTrue(viewModel.state.value.isNavigating)
        assertEquals(OnboardingEffect.NavigateToHome, viewModel.effects.first())
        coVerify(exactly = 1) { repository.completeOnboarding() }
    }

    @Test
    fun getStartedNavigatesHomeWhenSaveFails() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.completeOnboarding() } returns AppResult.Failure(AppError.Unknown)

        viewModel.onIntent(OnboardingIntent.GetStartedClicked)

        assertEquals(OnboardingEffect.NavigateToHome, viewModel.effects.first())
    }

    @Test
    fun repeatedTapsNavigateOnce() = runTest(mainDispatcherRule.dispatcher) {
        val effects = mutableListOf<OnboardingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }

        viewModel.onIntent(OnboardingIntent.GetStartedClicked)
        viewModel.onIntent(OnboardingIntent.GetStartedClicked)
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(OnboardingIntent.GetStartedClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf<OnboardingEffect>(OnboardingEffect.NavigateToHome), effects)
        coVerify(exactly = 1) { repository.completeOnboarding() }
    }
}
