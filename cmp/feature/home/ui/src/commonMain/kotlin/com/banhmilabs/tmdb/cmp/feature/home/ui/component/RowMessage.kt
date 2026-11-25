package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRowDefaults
import com.banhmilabs.tmdb.cmp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreview
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

@TmdbComponentPreviews
@Composable
private fun RowMessagePreview() {
    TmdbPreview {
        RowMessage(message = "Couldn't load this list.", onRetry = {}, modifier = Modifier.padding(TmdbSpacing.md))
    }
}

@Preview(name = "Without retry")
@Composable
private fun RowMessageWithoutRetryPreview() {
    TmdbPreview {
        RowMessage(message = "Nothing here yet.", onRetry = null, modifier = Modifier.padding(TmdbSpacing.md))
    }
}
