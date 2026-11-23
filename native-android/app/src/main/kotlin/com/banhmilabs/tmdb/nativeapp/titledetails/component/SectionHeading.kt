package com.banhmilabs.tmdb.nativeapp.titledetails.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val HeadingStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier.semantics { heading() }, style = HeadingStyle, maxLines = 1)
}
