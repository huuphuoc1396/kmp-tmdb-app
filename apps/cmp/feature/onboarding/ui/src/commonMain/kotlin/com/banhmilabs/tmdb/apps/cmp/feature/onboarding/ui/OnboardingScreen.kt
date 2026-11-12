package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.component.GetStartedButton
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.Res
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_artwork_placeholder
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_body
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_headline
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_scrim
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.tmdb_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal const val LOGO_TAG = "onboarding_logo"

private val ContentStartPadding = 29.dp
private val ContentEndPadding = 31.dp
private val ContentBottomPadding = 31.dp
private val ContentGap = 19.dp

private val HeadlineStyle = TextStyle(
    color = Color.White,
    fontSize = 32.sp,
    lineHeight = 37.3.sp,
    fontWeight = FontWeight.Light,
)
private val BodyStyle = TextStyle(
    color = Color.White,
    fontSize = 15.sp,
    lineHeight = 18.75.sp,
    fontWeight = FontWeight.Normal,
)

@Composable
internal fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToHome -> onFinished()
            }
        }
    }
    OnboardingContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_artwork_placeholder),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Image(
            painter = painterResource(Res.drawable.onboarding_scrim),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = ContentStartPadding, end = ContentEndPadding, bottom = ContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(ContentGap),
        ) {
            Image(
                painter = painterResource(Res.drawable.tmdb_logo),
                contentDescription = null,
                modifier = Modifier.testTag(LOGO_TAG),
            )
            Text(text = stringResource(Res.string.onboarding_headline), style = HeadlineStyle)
            Text(text = stringResource(Res.string.onboarding_body), style = BodyStyle)
            GetStartedButton(
                enabled = !state.isNavigating,
                onClick = { onIntent(OnboardingIntent.GetStartedClicked) },
            )
        }
    }
}
