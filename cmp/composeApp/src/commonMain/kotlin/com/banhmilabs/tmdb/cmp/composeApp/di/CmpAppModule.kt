package com.banhmilabs.tmdb.cmp.composeApp.di

import com.banhmilabs.tmdb.cmp.composeApp.AppViewModel
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.di.onboardingUiModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cmpAppModule = module {
    includes(onboardingUiModule)
    viewModelOf(::AppViewModel)
}
