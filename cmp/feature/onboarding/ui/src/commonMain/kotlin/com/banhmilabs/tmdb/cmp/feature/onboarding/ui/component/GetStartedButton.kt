package com.banhmilabs.tmdb.cmp.feature.onboarding.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.onboarding.ui.generated.resources.onboarding_get_started
import org.jetbrains.compose.resources.stringResource

internal const val GET_STARTED_TAG = "onboarding_get_started"

@Composable
internal fun GetStartedButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .testTag(GET_STARTED_TAG)
            .fillMaxWidth()
            .heightIn(min = TmdbSize.touchTarget)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.primary, colors.primaryContainer)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(TmdbSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_get_started),
            color = colors.onPrimary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
