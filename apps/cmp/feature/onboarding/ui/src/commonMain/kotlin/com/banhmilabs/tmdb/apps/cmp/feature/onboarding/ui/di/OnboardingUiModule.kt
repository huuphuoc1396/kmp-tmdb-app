package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.di

import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingUiModule = module {
    viewModelOf(::OnboardingViewModel)
}
