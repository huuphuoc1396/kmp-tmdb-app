package com.banhmilabs.tmdb.cmp.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.banhmilabs.tmdb.cmp.designsystem.generated.resources.Res
import com.banhmilabs.tmdb.cmp.designsystem.generated.resources.ic_person
import org.jetbrains.compose.resources.painterResource

private const val PERSON_ICON_ALPHA = 0.5f

/** A generic, decorative person icon in white at 50%, for avatars and missing photos. */
@Composable
fun PersonIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.ic_person),
        contentDescription = null,
        modifier = modifier,
        alpha = PERSON_ICON_ALPHA,
    )
}
