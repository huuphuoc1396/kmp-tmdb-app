package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.navigation.OnboardingRoute
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.navigation.onboardingScreen

@Composable
internal fun AppNavHost(startRoute: Any, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = startRoute) {
        onboardingScreen(
            onFinished = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            },
        )
        composable<HomeRoute> { PlaceholderHomeScreen() }
    }
}
