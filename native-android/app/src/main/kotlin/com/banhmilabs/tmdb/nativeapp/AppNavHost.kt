package com.banhmilabs.tmdb.nativeapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.favorites.navigation.FavoritesRoute
import com.banhmilabs.tmdb.nativeapp.favorites.navigation.favoritesScreen
import com.banhmilabs.tmdb.nativeapp.home.navigation.HomeRoute
import com.banhmilabs.tmdb.nativeapp.home.navigation.homeScreen
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.OnboardingRoute
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.onboardingScreen
import com.banhmilabs.tmdb.nativeapp.titledetails.navigation.titleDetailsScreen
import com.banhmilabs.tmdb.nativeapp.titledetails.navigation.toDetailsRoute

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    val openTitle: (Title) -> Unit = { navController.navigate(it.id.toDetailsRoute()) }
    val openTitleId: (TitleId) -> Unit = { navController.navigate(it.toDetailsRoute()) }
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        homeScreen(
            onTitleClick = openTitle,
            onFavoriteClick = openTitleId,
            onSeeAllFavorites = { navController.navigate(FavoritesRoute) },
        )
        favoritesScreen(onBack = navController::navigateUp, onTitleClick = openTitleId)
        titleDetailsScreen(onBack = navController::navigateUp, onTitleClick = openTitle)
    }
}
