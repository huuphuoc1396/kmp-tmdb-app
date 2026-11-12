package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.di

import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings of the onboarding feature. */
val onboardingUiModule = module {
    viewModelOf(::OnboardingViewModel)
}
