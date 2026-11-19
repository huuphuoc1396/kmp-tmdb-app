package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_title
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.ic_person
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TitleToAvatar = 8.dp
private val AvatarSize = 40.dp
private val AvatarTopOffset = 7.dp
private val PersonIconSize = 24.dp
private const val PERSON_ICON_ALPHA = 0.5f

private val TitleStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun HomeHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
            text = stringResource(Res.string.home_title),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = TitleStyle,
        )
        Spacer(modifier = Modifier.width(TitleToAvatar))
        Box(
            modifier = Modifier
                .padding(top = AvatarTopOffset)
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_person),
                contentDescription = null,
                modifier = Modifier.size(PersonIconSize),
                alpha = PERSON_ICON_ALPHA,
            )
        }
    }
}
