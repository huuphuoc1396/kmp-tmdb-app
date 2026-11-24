package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing

@Composable
internal fun RetryMessage(
    message: String,
    retryLabel: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TmdbSpacing.sm, alignment = Alignment.CenterVertically),
        horizontalAlignment = horizontalAlignment,
    ) {
        val textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
        Text(
            text = message,
            color = colors.onSurfaceVariant,
            textAlign = textAlign,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (onRetry != null) {
            Text(
                text = retryLabel,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(colors.secondary)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = TmdbSpacing.md, vertical = TmdbSpacing.xs),
                color = colors.onSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
