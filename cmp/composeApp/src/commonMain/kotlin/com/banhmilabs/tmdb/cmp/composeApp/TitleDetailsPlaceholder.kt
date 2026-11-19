package com.banhmilabs.tmdb.cmp.composeApp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.composeApp.generated.resources.Res
import com.banhmilabs.tmdb.cmp.composeApp.generated.resources.details_back
import com.banhmilabs.tmdb.cmp.composeApp.generated.resources.ic_arrow_back
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val NamePadding = 30.dp
private val NameStyle = TextStyle(
    color = Color.White,
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
)

/** Opens [TitleDetailsPlaceholderScreen] for the title named [name]. */
@Serializable
internal data class TitleDetailsPlaceholderRoute(val name: String)

/** Temporary title details: the name and a back button. Replaced by the details feature. */
@Composable
internal fun TitleDetailsPlaceholderScreen(name: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Image(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = stringResource(Res.string.details_back),
            )
        }
        Text(
            text = name,
            modifier = Modifier.align(Alignment.Center).padding(NamePadding),
            style = NameStyle,
        )
    }
}
