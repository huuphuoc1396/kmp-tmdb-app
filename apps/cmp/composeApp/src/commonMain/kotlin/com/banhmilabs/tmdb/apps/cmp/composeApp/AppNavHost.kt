package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.OnboardingRoute
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.onboardingScreen

/** The app's navigation graph, starting at [startDestination]. */
@Composable
internal fun AppNavHost(
    startDestination: StartDestination,
    navController: NavHostController = rememberNavController(),
) {
    val startRoute: Any = when (startDestination) {
        StartDestination.Onboarding -> OnboardingRoute
        StartDestination.Home -> HomeRoute
    }
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                // Onboarding leaves the back stack: Back on Home exits the app (AC-4).
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        composable<HomeRoute> { PlaceholderHomeScreen() }
    }
}
