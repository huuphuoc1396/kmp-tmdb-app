package com.banhmilabs.tmdb.cmp.feature.home.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.cmp.feature.home.ui.HomeScreen
import com.banhmilabs.tmdb.core.model.Title

fun NavGraphBuilder.homeScreen(onTitleClick: (Title) -> Unit) {
    composable<HomeRoute> { HomeScreen(onTitleClick = onTitleClick) }
}
