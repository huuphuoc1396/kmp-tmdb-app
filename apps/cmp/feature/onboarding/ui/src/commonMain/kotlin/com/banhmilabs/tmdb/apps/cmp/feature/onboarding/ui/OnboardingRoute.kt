package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import kotlinx.serialization.Serializable

/** Onboarding destination; only the app shell navigates here, so the route lives in `ui` (architecture 5.5). */
@Serializable
data object OnboardingRoute
