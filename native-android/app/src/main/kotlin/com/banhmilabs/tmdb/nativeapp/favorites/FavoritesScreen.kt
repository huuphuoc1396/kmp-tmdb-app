package com.banhmilabs.tmdb.nativeapp.favorites

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSize
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.RemoveFavoriteDialog
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreviews
import com.banhmilabs.tmdb.nativeapp.favorites.component.FavoriteCard
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import com.banhmilabs.tmdb.service.title.domain.TitleSummary
import org.koin.androidx.compose.koinViewModel

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
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        FavoritesTopBar(
            count = state.favorites.size,
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    start = TmdbSpacing.lg,
                    end = TmdbSpacing.lg,
                    top = insets.calculateTopPadding() + TmdbSpacing.xs,
                ),
        )
        val bottomPadding = insets.calculateBottomPadding() + TmdbSpacing.xl
        if (state.favorites.isEmpty()) {
            FavoritesEmpty(
                bottomPadding = bottomPadding,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            FavoritesList(
                favorites = state.favorites,
                listState = listState,
                bottomPadding = bottomPadding,
                onIntent = onIntent,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
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
private fun FavoritesEmpty(bottomPadding: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(
            start = TmdbSpacing.lg,
            end = TmdbSpacing.lg,
            top = TmdbSpacing.md,
            bottom = bottomPadding,
        ),
    ) {
        FavoritesIntro()
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.favorites_empty),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteTitle>,
    listState: LazyListState,
    bottomPadding: Dp,
    onIntent: (FavoritesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TmdbSpacing.lg,
            end = TmdbSpacing.lg,
            top = TmdbSpacing.md,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(TmdbSpacing.sm),
    ) {
        item(key = "intro") {
            FavoritesIntro()
        }
        items(favorites, key = { "${it.id.type}-${it.id.tmdbId}" }) { favorite ->
            FavoriteCard(
                favorite = favorite,
                onClick = { onIntent(FavoritesIntent.TitleClicked(favorite.id)) },
                onRemoveClick = { onIntent(FavoritesIntent.RemoveFavoriteClicked(favorite)) },
            )
        }
    }
}

@Composable
private fun FavoritesTopBar(count: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = TmdbSize.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button, onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_back_arrow),
                contentDescription = null,
                modifier = Modifier.size(TmdbSize.iconMedium),
            )
            Spacer(modifier = Modifier.width(TmdbSpacing.xs))
            Text(
                text = stringResource(R.string.favorites_back),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .heightIn(min = 28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = TmdbSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pluralStringResource(R.plurals.favorites_count, count, count),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun FavoritesIntro() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.favorites_title),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.xs))
        Text(
            text = stringResource(R.string.favorites_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.sm))
    }
}

private fun previewFavorite(id: Long, type: TitleType, name: String, year: Int?, genre: String?): FavoriteTitle {
    val titleId = TitleId(id, type)
    return FavoriteTitle(titleId, TitleSummary(titleId, name, posterUrl = null, releaseYear = year, firstGenre = genre))
}

internal val previewFavorites = listOf(
    previewFavorite(1, TitleType.MOVIE, "Dune: Part Two", 2024, "Science Fiction"),
    previewFavorite(2, TitleType.TV, "Shōgun", 2024, "Drama"),
    previewFavorite(3, TitleType.MOVIE, "The Wild Robot", null, null),
    FavoriteTitle(TitleId(4, TitleType.TV), summary = null),
)

@TmdbPreviews
@Composable
private fun FavoritesContentPreview() {
    TmdbPreview {
        FavoritesContent(state = FavoritesUiState(favorites = previewFavorites), onBack = {}, onIntent = {})
    }
}

@Preview(name = "Empty", device = Devices.PHONE)
@Composable
private fun FavoritesContentEmptyPreview() {
    TmdbPreview {
        FavoritesContent(state = FavoritesUiState(), onBack = {}, onIntent = {})
    }
}

@Preview(name = "Remove dialog", device = Devices.PHONE)
@Composable
private fun FavoritesContentRemoveDialogPreview() {
    TmdbPreview {
        FavoritesContent(
            state = FavoritesUiState(favorites = previewFavorites, pendingRemoval = previewFavorites.first()),
            onBack = {},
            onIntent = {},
        )
    }
}
