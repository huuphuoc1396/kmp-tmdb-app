package com.banhmilabs.tmdb.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.banhmilabs.tmdb.cmp.feature.home.ui.navigation.HomeRoute
import com.banhmilabs.tmdb.cmp.feature.home.ui.navigation.homeScreen
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.navigation.OnboardingRoute
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.navigation.onboardingScreen

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        homeScreen(
            onTitleClick = { title -> navController.navigate(TitleDetailsPlaceholderRoute(name = title.name)) },
        )
        composable<TitleDetailsPlaceholderRoute> { entry ->
            TitleDetailsPlaceholderScreen(
                name = entry.toRoute<TitleDetailsPlaceholderRoute>().name,
                onBack = navController::navigateUp,
            )
        }
    }
}
