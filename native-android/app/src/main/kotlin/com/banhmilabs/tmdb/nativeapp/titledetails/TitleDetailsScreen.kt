package com.banhmilabs.tmdb.nativeapp.titledetails

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterRow
import com.banhmilabs.tmdb.nativeapp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.nativeapp.titledetails.component.CastRow
import com.banhmilabs.tmdb.nativeapp.titledetails.component.DetailsArtwork
import com.banhmilabs.tmdb.nativeapp.titledetails.component.DetailsHeader
import com.banhmilabs.tmdb.nativeapp.titledetails.component.DetailsTopBar
import com.banhmilabs.tmdb.nativeapp.titledetails.component.GenreChips
import com.banhmilabs.tmdb.nativeapp.titledetails.component.SectionHeading
import com.banhmilabs.tmdb.nativeapp.titledetails.component.TrailerButton
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenPadding = 30.dp

private val HeaderTop = 376.dp
private val HeaderToDivider = 21.dp
private val DividerToSynopsis = 20.dp
private val SynopsisToTrailer = 30.dp
private val ToCastHeading = 46.dp
private val CastHeadingToRow = 16.dp
private val ToSectionHeading = 41.dp
private val HeadingToContent = 20.dp
private val BottomPadding = 40.dp
private const val DIVIDER_ALPHA = 0.48f

private val SynopsisStyle = TextStyle(color = DetailsColors.Synopsis, fontSize = 14.sp, lineHeight = 17.5.sp)

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
    Box(modifier = modifier.fillMaxSize().background(DetailsColors.Page)) {
        when (val load = state.load) {
            DetailsLoad.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
            )

            DetailsLoad.Error -> RetryMessage(
                message = stringResource(R.string.details_error),
                retryLabel = stringResource(R.string.details_retry),
                onRetry = { onIntent(TitleDetailsIntent.Retry) },
                modifier = Modifier.align(Alignment.Center).padding(horizontal = ScreenPadding),
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
                    .padding(top = HeaderTop)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = BottomPadding),
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
    val padded = Modifier.padding(horizontal = ScreenPadding)
    DetailsHeader(details = details, language = language, modifier = padded)
    Spacer(modifier = Modifier.height(HeaderToDivider))
    HorizontalDivider(modifier = padded, color = Color.Black.copy(alpha = DIVIDER_ALPHA))
    Spacer(modifier = Modifier.height(DividerToSynopsis))
    Text(
        text = details.synopsis ?: stringResource(R.string.details_no_synopsis),
        modifier = padded,
        style = SynopsisStyle,
    )
    if (details.trailer != null) {
        Spacer(modifier = Modifier.height(SynopsisToTrailer))
        TrailerButton(onClick = { onIntent(TitleDetailsIntent.TrailerClicked) }, modifier = padded)
    }
    if (details.cast.isNotEmpty()) {
        Spacer(modifier = Modifier.height(ToCastHeading))
        SectionHeading(text = stringResource(R.string.details_cast), modifier = padded)
        Spacer(modifier = Modifier.height(CastHeadingToRow))
        CastRow(cast = details.cast, startPadding = ScreenPadding, listState = scroll.cast)
    }
    if (details.genres.isNotEmpty()) {
        Spacer(modifier = Modifier.height(ToSectionHeading))
        SectionHeading(text = stringResource(R.string.details_genres), modifier = padded)
        Spacer(modifier = Modifier.height(HeadingToContent))
        GenreChips(genres = details.genres, modifier = padded)
    }
    if (details.recommendations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(ToSectionHeading))
        SectionHeading(text = stringResource(R.string.details_recommendations), modifier = padded)
        Spacer(modifier = Modifier.height(HeadingToContent))
        PosterRow(
            titles = details.recommendations,
            onTitleClick = { onIntent(TitleDetailsIntent.RecommendationClicked(it)) },
            listState = scroll.recommendations,
            contentPadding = PaddingValues(start = ScreenPadding),
        )
    }
}
