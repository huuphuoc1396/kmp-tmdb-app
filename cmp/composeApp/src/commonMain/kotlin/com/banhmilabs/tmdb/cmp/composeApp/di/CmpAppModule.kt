package com.banhmilabs.tmdb.cmp.composeApp.di

import com.banhmilabs.tmdb.cmp.composeApp.AppViewModel
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.di.favoritesUiModule
import com.banhmilabs.tmdb.cmp.feature.home.ui.di.homeUiModule
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.di.onboardingUiModule
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.di.titleDetailsUiModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cmpAppModule = module {
    includes(onboardingUiModule, homeUiModule, favoritesUiModule, titleDetailsUiModule)
    viewModelOf(::AppViewModel)
}
