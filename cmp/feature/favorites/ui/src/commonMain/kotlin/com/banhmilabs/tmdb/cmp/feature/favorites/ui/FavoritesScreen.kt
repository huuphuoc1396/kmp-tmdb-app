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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val ScreenPadding = 20.dp
private val TopPadding = 8.dp
private val BottomPadding = 24.dp
private val HeaderMinHeight = 48.dp
private val ArrowSize = 24.dp
private val ArrowToLabel = 8.dp
private val BadgeHeight = 28.dp
private val BadgePadding = 12.dp
private val HeaderToTitle = 16.dp
private val TitleToSubtitle = 8.dp
private val SubtitleToList = 26.dp
private val CardSpacing = 12.dp
private val EmptyTopPadding = 48.dp

private val BackStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
private val TitleStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
private val SubtitleStyle = TextStyle(color = FavoritesColors.SecondaryText, fontSize = 13.sp)
private val EmptyStyle = TextStyle(
    color = FavoritesColors.SecondaryText,
    fontSize = 14.sp,
    textAlign = TextAlign.Center,
)
private val BadgeStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)

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
        modifier = modifier.fillMaxSize().background(FavoritesColors.Background),
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = insets.calculateTopPadding() + TopPadding,
            bottom = insets.calculateBottomPadding() + BottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
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
                    modifier = Modifier.fillMaxWidth().padding(top = EmptyTopPadding),
                    style = EmptyStyle,
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
            modifier = Modifier.fillMaxWidth().heightIn(min = HeaderMinHeight),
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
                    modifier = Modifier.size(ArrowSize),
                )
                Spacer(modifier = Modifier.width(ArrowToLabel))
                Text(text = stringResource(Res.string.favorites_back), style = BackStyle)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(BadgeHeight)
                    .clip(CircleShape)
                    .background(FavoritesColors.Card)
                    .padding(horizontal = BadgePadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pluralStringResource(Res.plurals.favorites_count, count, count),
                    style = BadgeStyle.copy(color = MaterialTheme.colorScheme.secondary),
                )
            }
        }
        Spacer(modifier = Modifier.height(HeaderToTitle))
        Text(
            text = stringResource(Res.string.favorites_title),
            modifier = Modifier.semantics { heading() },
            style = TitleStyle,
        )
        Spacer(modifier = Modifier.height(TitleToSubtitle))
        Text(text = stringResource(Res.string.favorites_subtitle), style = SubtitleStyle)
        Spacer(modifier = Modifier.height(SubtitleToList - CardSpacing))
    }
}
