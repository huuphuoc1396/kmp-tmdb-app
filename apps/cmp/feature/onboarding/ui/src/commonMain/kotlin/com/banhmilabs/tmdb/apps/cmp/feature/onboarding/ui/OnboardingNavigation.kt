package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

/** Adds the onboarding screen; [onFinished] runs once onboarding is done and Home should replace it. */
fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> {
        val viewModel = koinViewModel<OnboardingViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    OnboardingEffect.NavigateToHome -> onFinished()
                }
            }
        }
        OnboardingScreen(state = state, onIntent = viewModel::onIntent)
    }
}
