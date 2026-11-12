package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.apps.cmp.designsystem.TmdbTheme

/** The cmp app: theme and navigation graph. Hosted by androidApp and iosApp. */
@Composable
fun App() {
    TmdbTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> { PlaceholderHomeScreen() }
        }
    }
}
