package com.banhmilabs.tmdb.nativeapp.onboarding

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.nativeapp.MainDispatcherRule
import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
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
        assertTrue(repository.observeOnboardingCompleted().first())
    }

    @Test
    fun getStartedNavigatesHomeWhenSaveFails() = runTest(mainDispatcherRule.dispatcher) {
        repository.failNextComplete(AppError.Unknown)

        viewModel.onIntent(OnboardingIntent.GetStartedClicked)

        assertEquals(OnboardingEffect.NavigateToHome, viewModel.effects.first())
        assertFalse(repository.observeOnboardingCompleted().first())
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
        assertTrue(repository.observeOnboardingCompleted().first())
    }
}
