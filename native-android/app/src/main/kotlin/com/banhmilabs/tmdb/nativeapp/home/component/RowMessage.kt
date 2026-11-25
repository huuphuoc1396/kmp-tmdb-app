package com.banhmilabs.tmdb.nativeapp.home.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterRowDefaults
import com.banhmilabs.tmdb.nativeapp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview

@Composable
internal fun RowMessage(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    RetryMessage(
        message = message,
        retryLabel = stringResource(R.string.home_retry),
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
