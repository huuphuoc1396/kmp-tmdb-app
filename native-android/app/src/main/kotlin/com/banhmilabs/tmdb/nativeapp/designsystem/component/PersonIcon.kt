package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview

@Composable
internal fun PersonIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_person),
        contentDescription = null,
        modifier = modifier,
        alpha = 0.5f,
    )
}

@Preview
@Composable
private fun PersonIconPreview() {
    TmdbPreview {
        PersonIcon(modifier = Modifier.padding(TmdbSpacing.md))
    }
}
