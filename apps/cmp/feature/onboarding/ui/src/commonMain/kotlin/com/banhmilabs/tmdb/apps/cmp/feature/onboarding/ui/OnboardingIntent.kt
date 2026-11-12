package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

internal sealed interface OnboardingIntent {
    data object GetStartedClicked : OnboardingIntent
}
