package com.banhmilabs.tmdb.nativeapp.home.di

import com.banhmilabs.tmdb.nativeapp.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    viewModelOf(::HomeViewModel)
}
