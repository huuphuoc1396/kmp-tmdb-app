package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

internal data class OnboardingUiState(val isNavigating: Boolean = false)

internal sealed interface OnboardingIntent {
    data object GetStartedClicked : OnboardingIntent
}

internal sealed interface OnboardingEffect {
    data object NavigateToHome : OnboardingEffect
}
