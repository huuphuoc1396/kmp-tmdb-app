package com.banhmilabs.tmdb.nativeapp.favorites.di

import com.banhmilabs.tmdb.nativeapp.favorites.FavoritesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val favoritesModule = module {
    viewModelOf(::FavoritesViewModel)
}
