package com.banhmilabs.tmdb.cmp.feature.favorites.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.RemoveFavoriteDialog
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.component.FavoriteCard
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_back
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_count
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_empty
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_subtitle
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_title
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.ic_back_arrow
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val BadgeMinHeight = 28.dp

@Composable
internal fun FavoritesScreen(
    onBack: () -> Unit,
    onTitleClick: (TitleId) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnTitleClick by rememberUpdatedState(onTitleClick)
    val language = Language.fromLocaleTag(Locale.current.toLanguageTag())
    LaunchedEffect(language) {
        viewModel.onIntent(FavoritesIntent.LanguageChanged(language))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FavoritesEffect.OpenTitle -> currentOnTitleClick(effect.id)
            }
        }
    }
    FavoritesContent(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

@Composable
internal fun FavoritesContent(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onIntent: (FavoritesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = TmdbSpacing.lg,
            end = TmdbSpacing.lg,
            top = insets.calculateTopPadding() + TmdbSpacing.xs,
            bottom = insets.calculateBottomPadding() + TmdbSpacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(TmdbSpacing.sm),
    ) {
        item(key = "header") {
            FavoritesHeader(count = state.favorites.size, onBack = onBack)
        }
        items(state.favorites, key = { "${it.id.type}-${it.id.tmdbId}" }) { favorite ->
            FavoriteCard(
                favorite = favorite,
                onClick = { onIntent(FavoritesIntent.TitleClicked(favorite.id)) },
                onRemoveClick = { onIntent(FavoritesIntent.RemoveFavoriteClicked(favorite)) },
            )
        }
        if (state.favorites.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(Res.string.favorites_empty),
                    modifier = Modifier.fillMaxWidth().padding(top = TmdbSpacing.xxxl),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    state.pendingRemoval?.let { pending ->
        RemoveFavoriteDialog(
            titleName = pending.summary?.name,
            onConfirm = { onIntent(FavoritesIntent.RemoveFavoriteConfirmed) },
            onDismiss = { onIntent(FavoritesIntent.RemoveFavoriteDismissed) },
        )
    }
}

@Composable
private fun FavoritesHeader(count: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = TmdbSize.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onBack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_back_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(TmdbSize.iconMedium),
                )
                Spacer(modifier = Modifier.width(TmdbSpacing.xs))
                Text(
                    text = stringResource(Res.string.favorites_back),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .heightIn(min = BadgeMinHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = TmdbSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pluralStringResource(Res.plurals.favorites_count, count, count),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Spacer(modifier = Modifier.height(TmdbSpacing.md))
        Text(
            text = stringResource(Res.string.favorites_title),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.xs))
        Text(
            text = stringResource(Res.string.favorites_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.sm))
    }
}
