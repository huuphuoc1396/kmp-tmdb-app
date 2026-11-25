package com.banhmilabs.tmdb.nativeapp.titledetails.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSize
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.FavoriteHeart

@Composable
internal fun DetailsTopBar(
    onBack: () -> Unit,
    isFavorite: Boolean?,
    onFavoriteClick: () -> Unit,
    isScrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = TmdbSize.touchTarget),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = TmdbSpacing.xs)
                .minimumInteractiveComponentSize()
                .heightIn(min = 32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = TmdbSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_back_arrow),
                contentDescription = null,
                modifier = Modifier.size(TmdbSize.iconMedium),
            )
            Spacer(modifier = Modifier.width(TmdbSpacing.xs))
            Text(
                text = stringResource(R.string.details_back),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (isFavorite != null) {
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = TmdbSpacing.xl),
            )
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.details_favorite)
    FavoriteHeart(
        isFavorite = isFavorite,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .toggleable(value = isFavorite, role = Role.Switch, onValueChange = { onClick() })
            .semantics { contentDescription = label },
    )
}
