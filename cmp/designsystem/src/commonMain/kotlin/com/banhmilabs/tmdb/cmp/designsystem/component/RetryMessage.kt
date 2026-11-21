package com.banhmilabs.tmdb.cmp.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/** A short message, vertically centered, with a "Try again" pill below it when [onRetry] is set. */
@Composable
fun RetryMessage(
    message: String,
    retryLabel: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MessageToRetry, alignment = Alignment.CenterVertically),
        horizontalAlignment = horizontalAlignment,
    ) {
        val textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
        Text(text = message, style = MessageStyle.copy(textAlign = textAlign))
        if (onRetry != null) {
            Text(
                text = retryLabel,
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
