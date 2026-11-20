package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.TitleDetailsScreen
import com.banhmilabs.tmdb.core.model.Title

fun NavGraphBuilder.titleDetailsScreen(onBack: () -> Unit, onTitleClick: (Title) -> Unit) {
    composable<TitleDetailsRoute> { entry ->
        TitleDetailsScreen(
            titleId = entry.toRoute<TitleDetailsRoute>().titleId,
            onBack = onBack,
            onTitleClick = onTitleClick,
        )
    }
}
