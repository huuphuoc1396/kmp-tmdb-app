package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRowDefaults
import com.banhmilabs.tmdb.cmp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RowMessage(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    RetryMessage(
        message = message,
        retryLabel = stringResource(Res.string.home_retry),
        onRetry = onRetry,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PosterRowDefaults.PosterHeight)
            .padding(horizontal = TmdbSpacing.xxl),
    )
}
