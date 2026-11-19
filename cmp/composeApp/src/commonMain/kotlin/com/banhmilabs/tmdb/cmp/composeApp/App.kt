package com.banhmilabs.tmdb.cmp.composeApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.banhmilabs.tmdb.cmp.designsystem.TmdbTheme
import com.banhmilabs.tmdb.cmp.feature.home.ui.navigation.HomeRoute
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.navigation.OnboardingRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    // Registered explicitly so remote images load on every platform, not only where service loaders work.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build()
    }
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
