package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

internal sealed interface OnboardingEffect {
    data object NavigateToHome : OnboardingEffect
}
