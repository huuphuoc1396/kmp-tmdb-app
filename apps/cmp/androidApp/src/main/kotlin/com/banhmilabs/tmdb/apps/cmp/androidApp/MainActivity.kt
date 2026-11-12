package com.banhmilabs.tmdb.apps.cmp.androidApp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.banhmilabs.tmdb.apps.cmp.composeApp.App
import com.banhmilabs.tmdb.apps.cmp.composeApp.AppViewModel
import com.banhmilabs.tmdb.apps.cmp.composeApp.StartDestination
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Hosts the shared Compose [App]. */
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.startDestination.value == StartDestination.Loading
        }
        // The app is always dark, so system bar icons stay light regardless of the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { App(appViewModel) }
    }
}
