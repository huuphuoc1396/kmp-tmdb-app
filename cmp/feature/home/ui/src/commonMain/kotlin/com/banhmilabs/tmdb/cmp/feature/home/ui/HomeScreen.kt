package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRow
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRowDefaults
import com.banhmilabs.tmdb.cmp.designsystem.component.RemoveFavoriteDialog
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreviews
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.CategoryTabs
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.FavoritesSection
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.HomeHeader
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.RowMessage
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.SearchField
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_background
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_header_glow
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_popular_heading
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_row_empty
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_row_error
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleSummary
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    onTitleClick: (Title) -> Unit,
    onFavoriteClick: (TitleId) -> Unit,
    onSeeAllFavorites: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnTitleClick by rememberUpdatedState(onTitleClick)
    val currentOnFavoriteClick by rememberUpdatedState(onFavoriteClick)
    val currentOnSeeAllFavorites by rememberUpdatedState(onSeeAllFavorites)
    val language = Language.fromLocaleTag(Locale.current.toLanguageTag())
    LaunchedEffect(language) {
        viewModel.onIntent(HomeIntent.LanguageChanged(language))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenTitle -> currentOnTitleClick(effect.title)
                is HomeEffect.OpenFavorite -> currentOnFavoriteClick(effect.id)
                HomeEffect.OpenFavorites -> currentOnSeeAllFavorites()
            }
        }
    }
    HomeContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun HomeContent(state: HomeUiState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val pullState = rememberPullToRefreshState()
    state.pendingRemoval?.let { pending ->
        RemoveFavoriteDialog(
            titleName = pending.summary?.name,
            onConfirm = { onIntent(HomeIntent.RemoveFavoriteConfirmed) },
            onDismiss = { onIntent(HomeIntent.RemoveFavoriteDismissed) },
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.home_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(HomeIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                    color = MaterialTheme.colorScheme.secondary,
                )
            },
        ) {
            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Image(
                    painter = painterResource(Res.drawable.home_header_glow),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(520.dp),
                    contentScale = ContentScale.FillBounds,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = TmdbSpacing.lg, bottom = TmdbSpacing.xxxl),
                ) {
                    HomeHeader(modifier = Modifier.padding(horizontal = TmdbSpacing.xxl))
                    Spacer(modifier = Modifier.height(TmdbSpacing.md))
                    SearchField(modifier = Modifier.padding(horizontal = TmdbSpacing.xxl).fillMaxWidth())
                    Spacer(modifier = Modifier.height(TmdbSpacing.xl))
                    CategoryTabs(
                        selected = state.selectedCategory,
                        onSelect = { onIntent(HomeIntent.CategorySelected(it)) },
                    )
                    Spacer(modifier = Modifier.height(TmdbSpacing.xxl))
                    HomeRows(state = state, onIntent = onIntent)
                }
            }
        }
    }
}

@Composable
private fun HomeRows(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    if (state.featured == RowState.Loading && state.popular == RowState.Loading) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = PosterRowDefaults.PosterHeight),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        Favorites(state = state, onIntent = onIntent)
        return
    }
    TitleRow(row = state.featured, category = state.selectedCategory, listType = TitleListType.FEATURED, onIntent)
    if (state.favoritesCount > 0) {
        Favorites(state = state, onIntent = onIntent)
        Spacer(modifier = Modifier.height(TmdbSpacing.xxl))
    } else {
        Spacer(modifier = Modifier.height(TmdbSpacing.xl))
    }
    Text(
        text = stringResource(Res.string.home_popular_heading),
        modifier = Modifier.padding(horizontal = TmdbSpacing.xxl).semantics { heading() },
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.height(TmdbSpacing.md))
    TitleRow(row = state.popular, category = state.selectedCategory, listType = TitleListType.POPULAR, onIntent)
}

@Composable
private fun Favorites(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    if (state.favoritesCount == 0) return
    Spacer(modifier = Modifier.height(TmdbSpacing.xxl))
    FavoritesSection(
        favorites = state.favorites,
        count = state.favoritesCount,
        onFavoriteClick = { onIntent(HomeIntent.FavoriteClicked(it)) },
        onRemoveClick = { onIntent(HomeIntent.RemoveFavoriteClicked(it)) },
        onSeeAll = { onIntent(HomeIntent.SeeAllFavoritesClicked) },
        modifier = Modifier.padding(horizontal = TmdbSpacing.xxl - TmdbSpacing.sm),
    )
}

@Composable
private fun TitleRow(row: RowState, category: TitleCategory, listType: TitleListType, onIntent: (HomeIntent) -> Unit) {
    val listState = rememberSaveable(category, saver = LazyListState.Saver) { LazyListState() }
    when (row) {
        RowState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().height(PosterRowDefaults.PosterHeight).padding(start = TmdbSpacing.xxl),
            contentAlignment = Alignment.CenterStart,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        RowState.Error -> RowMessage(
            message = stringResource(Res.string.home_row_error),
            onRetry = { onIntent(HomeIntent.Retry(listType)) },
        )

        is RowState.Titles -> if (row.titles.isEmpty()) {
            RowMessage(message = stringResource(Res.string.home_row_empty), onRetry = null)
        } else {
            PosterRow(
                titles = row.titles,
                onTitleClick = { onIntent(HomeIntent.TitleClicked(it)) },
                listState = listState,
                contentPadding = PaddingValues(start = TmdbSpacing.xxl),
            )
        }
    }
}

private val previewTitles = listOf(
    "Dune: Part Two",
    "The Wild Robot",
    "Inside Out 2",
    "Furiosa: A Mad Max Saga",
    "Kingdom of the Planet of the Apes",
).mapIndexed { index, name -> Title(TitleId(index + 1L, TitleType.MOVIE), name, posterUrl = null) }

internal val previewFavorites = previewTitles.take(3).map { title ->
    FavoriteTitle(
        id = title.id,
        summary = TitleSummary(title.id, title.name, posterUrl = null, releaseYear = 2024, firstGenre = "Drama"),
    )
}

@TmdbPreviews
@Composable
private fun HomeContentPreview() {
    TmdbPreview {
        HomeContent(
            state = HomeUiState(
                featured = RowState.Titles(previewTitles),
                popular = RowState.Titles(previewTitles.reversed()),
                favorites = previewFavorites,
                favoritesCount = previewFavorites.size,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Loading", device = Devices.PHONE)
@Composable
private fun HomeContentLoadingPreview() {
    TmdbPreview {
        HomeContent(state = HomeUiState(), onIntent = {})
    }
}

@Preview(name = "Errors", device = Devices.PHONE)
@Composable
private fun HomeContentErrorPreview() {
    TmdbPreview {
        HomeContent(state = HomeUiState(featured = RowState.Error, popular = RowState.Error), onIntent = {})
    }
}
