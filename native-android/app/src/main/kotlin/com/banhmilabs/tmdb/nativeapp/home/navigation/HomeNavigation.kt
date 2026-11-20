package com.banhmilabs.tmdb.nativeapp.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.nativeapp.home.HomeScreen

internal fun NavGraphBuilder.homeScreen(onTitleClick: (Title) -> Unit) {
    composable<HomeRoute> { HomeScreen(onTitleClick = onTitleClick) }
}
