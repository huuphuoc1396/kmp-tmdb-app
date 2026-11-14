package com.banhmilabs.tmdb.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbTheme
import com.banhmilabs.tmdb.nativeapp.onboarding.navigation.OnboardingRoute

@Composable
internal fun App(viewModel: AppViewModel) {
    TmdbTheme {
        val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (startDestination) {
                StartDestination.Loading -> Unit
                StartDestination.Onboarding -> AppNavHost(startRoute = OnboardingRoute)
                StartDestination.Home -> AppNavHost(startRoute = HomeRoute)
            }
        }
    }
}
