package com.banhmilabs.tmdb.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.navigation.FavoritesRoute
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.navigation.favoritesScreen
import com.banhmilabs.tmdb.cmp.feature.home.ui.navigation.HomeRoute
import com.banhmilabs.tmdb.cmp.feature.home.ui.navigation.homeScreen
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.navigation.OnboardingRoute
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.navigation.onboardingScreen
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.navigation.titleDetailsScreen
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.navigation.toDetailsRoute
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    val openTitle: (Title) -> Unit = { title -> navController.navigate(title.id.toDetailsRoute()) }
    val openTitleId: (TitleId) -> Unit = { id -> navController.navigate(id.toDetailsRoute()) }
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
