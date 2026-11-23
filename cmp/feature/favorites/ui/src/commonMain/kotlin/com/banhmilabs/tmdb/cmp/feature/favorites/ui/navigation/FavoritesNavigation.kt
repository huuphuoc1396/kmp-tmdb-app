package com.banhmilabs.tmdb.cmp.feature.favorites.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.FavoritesScreen
import com.banhmilabs.tmdb.core.model.TitleId

fun NavGraphBuilder.favoritesScreen(onBack: () -> Unit, onTitleClick: (TitleId) -> Unit) {
    composable<FavoritesRoute> { FavoritesScreen(onBack = onBack, onTitleClick = onTitleClick) }
}
