package com.banhmilabs.tmdb.nativeapp.onboarding.di

import com.banhmilabs.tmdb.nativeapp.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
