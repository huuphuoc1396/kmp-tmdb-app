package com.banhmilabs.tmdb.cmp.feature.home.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.cmp.feature.home.ui.HomeScreen
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId

fun NavGraphBuilder.homeScreen(
    onTitleClick: (Title) -> Unit,
    onFavoriteClick: (TitleId) -> Unit,
    onSeeAllFavorites: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            onTitleClick = onTitleClick,
            onFavoriteClick = onFavoriteClick,
            onSeeAllFavorites = onSeeAllFavorites,
        )
    }
}
