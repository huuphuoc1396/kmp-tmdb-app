package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.nativeapp.R

@Composable
internal fun FavoriteHeart(isFavorite: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(32.dp)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(if (isFavorite) colors.secondary else colors.onSurface),
        )
    }
}
