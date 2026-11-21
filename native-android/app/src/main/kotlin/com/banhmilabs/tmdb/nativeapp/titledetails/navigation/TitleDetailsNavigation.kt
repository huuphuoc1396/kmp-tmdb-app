package com.banhmilabs.tmdb.nativeapp.titledetails.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.nativeapp.titledetails.TitleDetailsScreen

internal fun NavGraphBuilder.titleDetailsScreen(onBack: () -> Unit, onTitleClick: (Title) -> Unit) {
    composable<TitleDetailsRoute> { entry ->
        TitleDetailsScreen(
            titleId = entry.toRoute<TitleDetailsRoute>().titleId,
            onBack = onBack,
            onTitleClick = onTitleClick,
        )
    }
}
