package com.banhmilabs.tmdb.nativeapp.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterRow
import com.banhmilabs.tmdb.nativeapp.designsystem.component.RemoveFavoriteDialog
import com.banhmilabs.tmdb.nativeapp.home.component.CategoryTabs
import com.banhmilabs.tmdb.nativeapp.home.component.FavoritesSection
import com.banhmilabs.tmdb.nativeapp.home.component.HomeHeader
import com.banhmilabs.tmdb.nativeapp.home.component.RowMessage
import com.banhmilabs.tmdb.nativeapp.home.component.RowMinHeight
import com.banhmilabs.tmdb.nativeapp.home.component.SearchField
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import org.koin.androidx.compose.koinViewModel

internal val ScreenPadding = 30.dp

private val ContentTopPadding = 20.dp
private val ContentBottomPadding = 79.dp
private val HeaderToSearch = 17.dp

private val SearchToTabs = 24.dp
private val TabsToFeatured = 30.dp
private val FeaturedToHeading = 23.dp
private val FeaturedToFavorites = 28.dp
private val FavoritesToHeading = 32.dp
private val FavoritesCardInset = 12.dp
private val HeadingToPopular = 16.dp
private val HeaderGlowHeight = 522.dp
private val RowSpinnerSize = 24.dp

private val HeadingStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
            painter = painterResource(R.drawable.home_background),
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
                    painter = painterResource(R.drawable.home_header_glow),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(HeaderGlowHeight),
                    contentScale = ContentScale.FillBounds,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = ContentTopPadding, bottom = ContentBottomPadding),
                ) {
                    HomeHeader(modifier = Modifier.padding(horizontal = ScreenPadding))
                    Spacer(modifier = Modifier.height(HeaderToSearch))
                    SearchField(modifier = Modifier.padding(horizontal = ScreenPadding).fillMaxWidth())
                    Spacer(modifier = Modifier.height(SearchToTabs))
                    CategoryTabs(
                        selected = state.selectedCategory,
                        onSelect = { onIntent(HomeIntent.CategorySelected(it)) },
                    )
                    Spacer(modifier = Modifier.height(TabsToFeatured))
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
            modifier = Modifier.fillMaxWidth().heightIn(min = RowMinHeight),
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
        Spacer(modifier = Modifier.height(FavoritesToHeading))
    } else {
        Spacer(modifier = Modifier.height(FeaturedToHeading))
    }
    Text(
        text = stringResource(R.string.home_popular_heading),
        modifier = Modifier.padding(horizontal = ScreenPadding).semantics { heading() },
        style = HeadingStyle,
    )
    Spacer(modifier = Modifier.height(HeadingToPopular))
    TitleRow(row = state.popular, category = state.selectedCategory, listType = TitleListType.POPULAR, onIntent)
}

@Composable
private fun Favorites(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    if (state.favoritesCount == 0) return
    Spacer(modifier = Modifier.height(FeaturedToFavorites))
    FavoritesSection(
        favorites = state.favorites,
        count = state.favoritesCount,
        onFavoriteClick = { onIntent(HomeIntent.FavoriteClicked(it)) },
        onRemoveClick = { onIntent(HomeIntent.RemoveFavoriteClicked(it)) },
        onSeeAll = { onIntent(HomeIntent.SeeAllFavoritesClicked) },
        modifier = Modifier.padding(horizontal = ScreenPadding - FavoritesCardInset),
    )
}

@Composable
private fun TitleRow(row: RowState, category: TitleCategory, listType: TitleListType, onIntent: (HomeIntent) -> Unit) {
    val listState = rememberSaveable(category, saver = LazyListState.Saver) { LazyListState() }
    when (row) {
        RowState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().height(RowMinHeight).padding(start = ScreenPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(RowSpinnerSize),
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        RowState.Error -> RowMessage(
            message = stringResource(R.string.home_row_error),
            onRetry = { onIntent(HomeIntent.Retry(listType)) },
        )

        is RowState.Titles -> if (row.titles.isEmpty()) {
            RowMessage(message = stringResource(R.string.home_row_empty), onRetry = null)
        } else {
            PosterRow(
                titles = row.titles,
                onTitleClick = { onIntent(HomeIntent.TitleClicked(it)) },
                listState = listState,
                contentPadding = PaddingValues(start = ScreenPadding),
            )
        }
    }
}
