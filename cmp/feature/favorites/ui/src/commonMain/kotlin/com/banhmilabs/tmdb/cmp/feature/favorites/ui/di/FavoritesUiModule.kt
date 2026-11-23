package com.banhmilabs.tmdb.cmp.feature.favorites.ui.di

import com.banhmilabs.tmdb.cmp.feature.favorites.ui.FavoritesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val favoritesUiModule = module {
    viewModelOf(::FavoritesViewModel)
}
