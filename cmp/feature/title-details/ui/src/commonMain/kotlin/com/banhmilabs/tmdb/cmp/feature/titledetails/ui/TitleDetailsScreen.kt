package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRow
import com.banhmilabs.tmdb.cmp.designsystem.component.RemoveFavoriteDialog
import com.banhmilabs.tmdb.cmp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.CastRow
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.DetailsArtwork
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.DetailsHeader
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.DetailsTopBar
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.GenreChips
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.SectionHeading
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component.TrailerButton
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_cast
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_error
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_genres
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_no_synopsis
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_recommendations
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_retry
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun TitleDetailsScreen(
    titleId: TitleId,
    onBack: () -> Unit,
    onTitleClick: (Title) -> Unit,
    viewModel: TitleDetailsViewModel = koinViewModel { parametersOf(titleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnTitleClick by rememberUpdatedState(onTitleClick)
    val uriHandler = LocalUriHandler.current
    val language = Language.fromLocaleTag(Locale.current.toLanguageTag())
    LaunchedEffect(language) {
        viewModel.onIntent(TitleDetailsIntent.LanguageChanged(language))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TitleDetailsEffect.NavigateBack -> currentOnBack()
                is TitleDetailsEffect.OpenTitle -> currentOnTitleClick(effect.title)
                is TitleDetailsEffect.OpenTrailer -> runCatching { uriHandler.openUri(effect.url) }
            }
        }
    }
    TitleDetailsContent(state = state, language = language, onIntent = viewModel::onIntent)
}

@Composable
internal fun TitleDetailsContent(
    state: TitleDetailsUiState,
    language: Language,
    onIntent: (TitleDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = DetailsScroll(
        screen = rememberScrollState(),
        cast = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
        recommendations = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    )
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (val load = state.load) {
            DetailsLoad.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
            )

            DetailsLoad.Error -> RetryMessage(
                message = stringResource(Res.string.details_error),
                retryLabel = stringResource(Res.string.details_retry),
                onRetry = { onIntent(TitleDetailsIntent.Retry) },
                modifier = Modifier.align(Alignment.Center).padding(horizontal = TmdbSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            )

            is DetailsLoad.Loaded -> LoadedContent(
                details = load.details,
                isRefreshing = state.isRefreshing,
                language = language,
                scroll = scroll,
                onIntent = onIntent,
            )
        }
        DetailsTopBar(
            onBack = { onIntent(TitleDetailsIntent.BackClicked) },
            isFavorite = state.isFavorite.takeIf { state.load is DetailsLoad.Loaded },
            onFavoriteClick = { onIntent(TitleDetailsIntent.FavoriteClicked) },
        )
    }
    if (state.isRemoveConfirmationShown) {
        RemoveFavoriteDialog(
            titleName = state.details?.name,
            onConfirm = { onIntent(TitleDetailsIntent.RemoveFavoriteConfirmed) },
            onDismiss = { onIntent(TitleDetailsIntent.RemoveFavoriteDismissed) },
        )
    }
}

private class DetailsScroll(val screen: ScrollState, val cast: LazyListState, val recommendations: LazyListState)

@Composable
private fun LoadedContent(
    details: TitleDetails,
    isRefreshing: Boolean,
    language: Language,
    scroll: DetailsScroll,
    onIntent: (TitleDetailsIntent) -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onIntent(TitleDetailsIntent.Refresh) },
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll.screen)) {
            DetailsArtwork(url = details.artworkUrl)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 376.dp)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = TmdbSpacing.xxxl),
            ) {
                DetailsBody(details = details, language = language, scroll = scroll, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun DetailsBody(
    details: TitleDetails,
    language: Language,
    scroll: DetailsScroll,
    onIntent: (TitleDetailsIntent) -> Unit,
) {
    val padded = Modifier.padding(horizontal = TmdbSpacing.xxl)
    DetailsHeader(details = details, language = language, modifier = padded)
    Spacer(modifier = Modifier.height(TmdbSpacing.lg))
    HorizontalDivider(modifier = padded, color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(modifier = Modifier.height(TmdbSpacing.lg))
    Text(
        text = details.synopsis ?: stringResource(Res.string.details_no_synopsis),
        modifier = padded,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
    if (details.trailer != null) {
        Spacer(modifier = Modifier.height(TmdbSpacing.xxl))
        TrailerButton(onClick = { onIntent(TitleDetailsIntent.TrailerClicked) }, modifier = padded)
    }
    if (details.cast.isNotEmpty()) {
        Spacer(modifier = Modifier.height(TmdbSpacing.xxxl))
        SectionHeading(text = stringResource(Res.string.details_cast), modifier = padded)
        Spacer(modifier = Modifier.height(TmdbSpacing.md))
        CastRow(cast = details.cast, startPadding = TmdbSpacing.xxl, listState = scroll.cast)
    }
    if (details.genres.isNotEmpty()) {
        Spacer(modifier = Modifier.height(TmdbSpacing.xxxl))
        SectionHeading(text = stringResource(Res.string.details_genres), modifier = padded)
        Spacer(modifier = Modifier.height(TmdbSpacing.lg))
        GenreChips(genres = details.genres, modifier = padded)
    }
    if (details.recommendations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(TmdbSpacing.xxxl))
        SectionHeading(text = stringResource(Res.string.details_recommendations), modifier = padded)
        Spacer(modifier = Modifier.height(TmdbSpacing.lg))
        PosterRow(
            titles = details.recommendations,
            onTitleClick = { onIntent(TitleDetailsIntent.RecommendationClicked(it)) },
            listState = scroll.recommendations,
            contentPadding = PaddingValues(start = TmdbSpacing.xxl),
        )
    }
}
