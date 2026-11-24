package com.banhmilabs.tmdb.nativeapp.di

import com.banhmilabs.tmdb.nativeapp.AppViewModel
import com.banhmilabs.tmdb.nativeapp.favorites.di.favoritesModule
import com.banhmilabs.tmdb.nativeapp.home.di.homeModule
import com.banhmilabs.tmdb.nativeapp.onboarding.di.onboardingModule
import com.banhmilabs.tmdb.nativeapp.titledetails.di.titleDetailsModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val nativeAppModule = module {
    includes(onboardingModule, homeModule, favoritesModule, titleDetailsModule)
    viewModelOf(::AppViewModel)
}
