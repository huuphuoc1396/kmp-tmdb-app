package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.feature.home.ui.ScreenPadding
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_retry
import org.jetbrains.compose.resources.stringResource

/** Same height as a poster row, so the layout doesn't jump when a row has no posters. */
internal val RowMinHeight = 188.dp
private val MessageToRetry = 12.dp
private val RetryShape = RoundedCornerShape(70.dp)
private val RetryHorizontalPadding = 14.dp
private val RetryVerticalPadding = 6.dp
private const val MESSAGE_ALPHA = 0.5f

private val MessageStyle = TextStyle(
    color = Color.White.copy(alpha = MESSAGE_ALPHA),
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
)
private val RetryStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
internal fun RowMessage(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MessageToRetry, alignment = Alignment.CenterVertically),
    ) {
        Text(text = message, style = MessageStyle)
        if (onRetry != null) {
            Text(
                text = stringResource(Res.string.home_retry),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RetryShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = RetryHorizontalPadding, vertical = RetryVerticalPadding),
                style = RetryStyle,
            )
        }
    }
}
