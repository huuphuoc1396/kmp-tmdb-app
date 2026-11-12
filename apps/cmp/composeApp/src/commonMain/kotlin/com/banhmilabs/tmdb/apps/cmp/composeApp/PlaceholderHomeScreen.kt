package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Temporary Home; replaced by the Home feature. Its label is not localized for that reason. */
@Composable
internal fun PlaceholderHomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text("Home", color = MaterialTheme.colorScheme.onBackground)
    }
}
