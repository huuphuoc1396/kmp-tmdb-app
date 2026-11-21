package com.banhmilabs.tmdb.nativeapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.nativeapp.home.navigation.HomeRoute
import com.banhmilabs.tmdb.nativeapp.home.navigation.homeScreen
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.OnboardingRoute
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.onboardingScreen
import com.banhmilabs.tmdb.nativeapp.titledetails.navigation.titleDetailsScreen
import com.banhmilabs.tmdb.nativeapp.titledetails.navigation.toDetailsRoute

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    // From Home and from recommendations; the same title may be stacked more than once.
    val openTitle: (Title) -> Unit = { navController.navigate(it.id.toDetailsRoute()) }
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        homeScreen(onTitleClick = openTitle)
        titleDetailsScreen(onBack = navController::navigateUp, onTitleClick = openTitle)
    }
}
