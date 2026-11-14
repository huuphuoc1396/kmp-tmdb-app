package com.banhmilabs.tmdb.nativeapp.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.onboarding.component.GetStartedButton
import org.koin.androidx.compose.koinViewModel

private val ContentStartPadding = 29.dp
private val ContentEndPadding = 31.dp
private val ContentBottomPadding = 31.dp
private val ContentGap = 19.dp
private val ContentMaxWidth = 315.dp

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
            painter = painterResource(R.drawable.onboarding_artwork_placeholder),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painterResource(R.drawable.onboarding_scrim),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = ContentStartPadding, end = ContentEndPadding, bottom = ContentBottomPadding),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ContentGap),
                ) {
                    Image(
                        painter = painterResource(R.drawable.tmdb_logo),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_headline),
                        modifier = Modifier.semantics { heading() },
                        style = HeadlineStyle,
                    )
                    Text(text = stringResource(R.string.onboarding_body), style = BodyStyle)
                    GetStartedButton(
                        enabled = !state.isNavigating,
                        onClick = { onIntent(OnboardingIntent.GetStartedClicked) },
                    )
                }
            }
        }
    }
}
