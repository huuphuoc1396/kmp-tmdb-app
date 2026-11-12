package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.apps.cmp.designsystem.TmdbTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * The cmp app: theme and navigation graph. Hosted by androidApp and iosApp. Draws only the background until the
 * start destination is known, so the wrong first screen never flashes (AC-3).
 */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    TmdbTheme {
        val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            startDestination?.let { AppNavHost(startDestination = it) }
        }
    }
}
