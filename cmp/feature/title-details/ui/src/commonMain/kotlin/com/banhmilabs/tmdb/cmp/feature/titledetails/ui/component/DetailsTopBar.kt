package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.designsystem.component.FavoriteHeart
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.DetailsColors
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_back
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_favorite
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.ic_back_arrow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val BarHeight = 48.dp

private val BackStart = 8.dp
private val PillHeight = 32.dp
private val PillPadding = 10.dp
private val ArrowSize = 24.dp
private val ArrowToLabel = 6.dp
private const val PILL_ALPHA = 0.65f

private val FavoriteEnd = 22.dp

private val LabelStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun DetailsTopBar(
    onBack: () -> Unit,
    isFavorite: Boolean?,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = BarHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = BackStart)
                .minimumInteractiveComponentSize()
                .heightIn(min = PillHeight)
                .clip(CircleShape)
                .background(DetailsColors.Track.copy(alpha = PILL_ALPHA))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = PillPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_back_arrow),
                contentDescription = null,
                modifier = Modifier.size(ArrowSize),
            )
            Spacer(modifier = Modifier.width(ArrowToLabel))
            Text(text = stringResource(Res.string.details_back), style = LabelStyle)
        }
        if (isFavorite != null) {
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = FavoriteEnd),
            )
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.details_favorite)
    FavoriteHeart(
        isFavorite = isFavorite,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .toggleable(value = isFavorite, role = Role.Switch, onValueChange = { onClick() })
            .semantics { contentDescription = label },
    )
}
