package com.banhmilabs.tmdb.nativeapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.banhmilabs.tmdb.nativeapp.home.navigation.HomeRoute
import com.banhmilabs.tmdb.nativeapp.home.navigation.homeScreen
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.OnboardingRoute
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.onboardingScreen

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        homeScreen(onTitleClick = { navController.navigate(TitleDetailsPlaceholderRoute(it.name)) })
        composable<TitleDetailsPlaceholderRoute> { entry ->
            TitleDetailsPlaceholderScreen(
                name = entry.toRoute<TitleDetailsPlaceholderRoute>().name,
                onBack = navController::navigateUp,
            )
        }
    }
}
