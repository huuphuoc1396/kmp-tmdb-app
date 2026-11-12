package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.Res
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_body
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_get_started
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.onboarding_headline
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.generated.resources.tmdb_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal const val GET_STARTED_TAG = "onboarding_get_started"
internal const val LOGO_TAG = "onboarding_logo"

// Values from docs/features/onboarding/design/onboarding.md.
private val ArtworkTop = Color(0xFF303243)
private val ArtworkBottom = Color(0xFF3D458F)
private const val ARTWORK_BOTTOM_STOP = 0.56527f
private val ScrimColor = Color(0xD915151D)
private const val SCRIM_START_STOP = 0.3089f
private const val SCRIM_END_STOP = 0.62685f
private val ContentStartPadding = 29.dp
private val ContentEndPadding = 31.dp
private val ContentBottomPadding = 31.dp
private val ContentGap = 19.dp
private val ButtonHeight = 43.dp
private val ButtonPadding = 12.dp

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
private val ButtonLabelStyle = TextStyle(
    color = Color.White,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
)

/** The onboarding screen: connects [OnboardingViewModel] and turns its effects into [onFinished]. */
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

/** Stateless onboarding UI. Taps on the button are ignored while [OnboardingUiState.isNavigating]. */
@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Placeholder until licensed artwork is supplied.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to ArtworkTop, ARTWORK_BOTTOM_STOP to ArtworkBottom),
            ),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    SCRIM_START_STOP to Color.Transparent,
                    SCRIM_END_STOP to ScrimColor,
                    1f to ScrimColor,
                ),
            ),
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
            Text(
                text = boldMarkupToAnnotatedString(stringResource(Res.string.onboarding_headline)),
                style = HeadlineStyle,
            )
            Text(text = stringResource(Res.string.onboarding_body), style = BodyStyle)
            GetStartedButton(
                enabled = !state.isNavigating,
                onClick = { onIntent(OnboardingIntent.GetStartedClicked) },
            )
        }
    }
}

@Composable
private fun GetStartedButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .testTag(GET_STARTED_TAG)
            .fillMaxWidth()
            .height(ButtonHeight)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.primary, colors.primaryContainer)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(ButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(Res.string.onboarding_get_started), style = ButtonLabelStyle, maxLines = 1)
    }
}
