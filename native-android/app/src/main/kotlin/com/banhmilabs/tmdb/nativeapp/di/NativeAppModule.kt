package com.banhmilabs.tmdb.nativeapp.di

import com.banhmilabs.tmdb.nativeapp.AppViewModel
import com.banhmilabs.tmdb.nativeapp.onboarding.di.onboardingModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val nativeAppModule = module {
    includes(onboardingModule)
    viewModelOf(::AppViewModel)
}
