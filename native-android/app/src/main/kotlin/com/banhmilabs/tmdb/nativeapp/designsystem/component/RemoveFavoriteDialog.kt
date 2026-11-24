package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.banhmilabs.tmdb.nativeapp.R

private val DialogColor = Color(0xFF211A30)
private val MessageColor = Color(0xFFB0AABF)
private val CancelBorderColor = Color(0xFF655F74)

private val ScreenMargin = 20.dp
private val DialogShape = RoundedCornerShape(20.dp)
private val ContentPadding = 24.dp
private val BottomPadding = 20.dp
private val TitleToMessage = 20.dp
private val MessageToButtons = 24.dp
private val ButtonSpacing = 12.dp
private val ButtonHeight = 48.dp
private val ButtonShape = RoundedCornerShape(10.dp)

private val TitleStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
private val MessageStyle = TextStyle(color = MessageColor, fontSize = 14.sp, lineHeight = 20.sp)
private val ButtonStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
internal fun RemoveFavoriteDialog(titleName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = ScreenMargin)
                .fillMaxWidth()
                .clip(DialogShape)
                .background(DialogColor)
                .verticalScroll(rememberScrollState())
                .padding(start = ContentPadding, end = ContentPadding, top = ContentPadding, bottom = BottomPadding),
        ) {
            Text(
                text = stringResource(R.string.remove_favorite_title),
                modifier = Modifier.semantics { heading() },
                style = TitleStyle,
            )
            Spacer(modifier = Modifier.height(TitleToMessage))
            Text(
                text = if (titleName != null) {
                    stringResource(R.string.remove_favorite_message, titleName)
                } else {
                    stringResource(R.string.remove_favorite_message_unknown)
                },
                style = MessageStyle,
            )
            Spacer(modifier = Modifier.height(MessageToButtons))
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonSpacing)) {
                DialogButton(
                    label = stringResource(R.string.remove_favorite_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).border(1.dp, CancelBorderColor, ButtonShape),
                )
                DialogButton(
                    label = stringResource(R.string.remove_favorite_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.secondary, ButtonShape),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(ButtonHeight)
            .clip(ButtonShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = ButtonStyle)
    }
}
