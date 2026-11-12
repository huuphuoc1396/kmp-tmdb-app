package com.banhmilabs.tmdb.apps.cmp.composeApp.di

import com.banhmilabs.tmdb.apps.cmp.composeApp.AppViewModel
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.di.onboardingUiModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings of the cmp app. */
val cmpAppModule = module {
    includes(onboardingUiModule)
    viewModelOf(::AppViewModel)
}
