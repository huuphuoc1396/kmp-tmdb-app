package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSize
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing

@Composable
internal fun RemoveFavoriteDialog(titleName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = TmdbSpacing.lg)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(colors.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(start = TmdbSpacing.xl, end = TmdbSpacing.xl, top = TmdbSpacing.xl, bottom = TmdbSpacing.lg),
        ) {
            Text(
                text = stringResource(R.string.remove_favorite_title),
                modifier = Modifier.semantics { heading() },
                color = colors.onSurface,
                style = typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(TmdbSpacing.lg))
            Text(
                text = if (titleName != null) {
                    stringResource(R.string.remove_favorite_message, titleName)
                } else {
                    stringResource(R.string.remove_favorite_message_unknown)
                },
                color = colors.onSurfaceVariant,
                style = typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(TmdbSpacing.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.sm)) {
                DialogButton(
                    label = stringResource(R.string.remove_favorite_cancel),
                    labelColor = colors.onSurface,
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(TmdbSize.borderWidth, colors.outline), MaterialTheme.shapes.medium),
                )
                DialogButton(
                    label = stringResource(R.string.remove_favorite_confirm),
                    labelColor = colors.onSecondary,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).background(colors.secondary, MaterialTheme.shapes.medium),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, labelColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = TmdbSize.touchTarget)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = labelColor, style = MaterialTheme.typography.titleMedium)
    }
}
