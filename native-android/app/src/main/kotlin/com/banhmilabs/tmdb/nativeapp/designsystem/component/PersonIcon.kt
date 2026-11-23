package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.banhmilabs.tmdb.nativeapp.R

private const val PERSON_ICON_ALPHA = 0.5f

@Composable
internal fun PersonIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_person),
        contentDescription = null,
        modifier = modifier,
        alpha = PERSON_ICON_ALPHA,
    )
}
