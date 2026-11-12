package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Adds the onboarding screen; [onFinished] runs once onboarding is done and Home should replace it. */
fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onFinished = onFinished) }
}
