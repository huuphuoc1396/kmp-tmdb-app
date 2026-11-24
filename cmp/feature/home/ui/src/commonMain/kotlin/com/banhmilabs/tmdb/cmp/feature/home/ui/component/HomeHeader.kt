package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PersonIcon
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
            text = stringResource(Res.string.home_title),
            modifier = Modifier.weight(1f).semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.width(TmdbSpacing.xs))
        Box(
            modifier = Modifier
                .padding(top = TmdbSpacing.xs)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            PersonIcon(modifier = Modifier.size(TmdbSize.iconMedium))
        }
    }
}
