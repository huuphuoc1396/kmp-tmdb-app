package com.banhmilabs.tmdb.cmp.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.generated.resources.Res
import com.banhmilabs.tmdb.cmp.designsystem.generated.resources.ic_person
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreview
import org.jetbrains.compose.resources.painterResource

@Composable
fun PersonIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.ic_person),
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
