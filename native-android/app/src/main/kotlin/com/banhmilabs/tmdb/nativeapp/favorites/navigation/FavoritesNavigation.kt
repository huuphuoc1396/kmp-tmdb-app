package com.banhmilabs.tmdb.nativeapp.favorites.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.favorites.FavoritesScreen

fun NavGraphBuilder.favoritesScreen(onBack: () -> Unit, onTitleClick: (TitleId) -> Unit) {
    composable<FavoritesRoute> { FavoritesScreen(onBack = onBack, onTitleClick = onTitleClick) }
}
