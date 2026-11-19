package com.banhmilabs.tmdb.cmp.feature.home.ui.di

import com.banhmilabs.tmdb.cmp.feature.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeUiModule = module {
    viewModelOf(::HomeViewModel)
}
