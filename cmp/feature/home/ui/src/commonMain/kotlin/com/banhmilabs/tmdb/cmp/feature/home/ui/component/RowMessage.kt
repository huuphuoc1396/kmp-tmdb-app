package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.cmp.feature.home.ui.ScreenPadding
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_retry
import org.jetbrains.compose.resources.stringResource

/** Same height as a poster row, so the layout doesn't jump when a row has no posters. */
internal val RowMinHeight = 188.dp

@Composable
internal fun RowMessage(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    RetryMessage(
        message = message,
        retryLabel = stringResource(Res.string.home_retry),
        onRetry = onRetry,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = ScreenPadding),
    )
}
