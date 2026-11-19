package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.CategoryTabs
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.HomeHeader
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.PosterRow
import com.banhmilabs.tmdb.cmp.feature.home.ui.component.SearchField
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_popular_heading
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal val ScreenPadding = 30.dp

/** The design's 64dp title offset, less a 44dp status bar that the safe-drawing inset already covers. */
private val ContentTopPadding = 20.dp
private val ContentBottomPadding = 79.dp
private val HeaderToSearch = 17.dp
private val SearchToTabs = 31.dp
private val TabsToFeatured = 37.dp
private val FeaturedToHeading = 23.dp
private val HeadingToPopular = 16.dp
private val HeaderGlowHeight = 522.dp

private val ScreenTop = Color(0xFF303243)
private const val SCREEN_GRADIENT_END = 0.65394f

private val HeadingStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun HomeScreen(onTitleClick: (Title) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnTitleClick by rememberUpdatedState(onTitleClick)
    val language = Language.fromLocaleTag(Locale.current.toLanguageTag())
    LaunchedEffect(language) {
        viewModel.onIntent(HomeIntent.LanguageChanged(language))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenTitle -> currentOnTitleClick(effect.title)
            }
        }
    }
    HomeContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun HomeContent(state: HomeUiState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to ScreenTop, SCREEN_GRADIENT_END to colors.background)),
    ) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderGlowHeight)
                    .background(Brush.verticalGradient(listOf(colors.primary, Color.Transparent))),
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
                PosterRow(titles = state.featured, onTitleClick = { onIntent(HomeIntent.TitleClicked(it)) })
                Spacer(modifier = Modifier.height(FeaturedToHeading))
                Text(
                    text = stringResource(Res.string.home_popular_heading),
                    modifier = Modifier.padding(start = ScreenPadding).semantics { heading() },
                    style = HeadingStyle,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(HeadingToPopular))
                PosterRow(titles = state.popular, onTitleClick = { onIntent(HomeIntent.TitleClicked(it)) })
            }
        }
    }
}
