package com.banhmilabs.tmdb.nativeapp.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.nativeapp.onboarding.OnboardingScreen

internal fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onFinished = onFinished) }
}
