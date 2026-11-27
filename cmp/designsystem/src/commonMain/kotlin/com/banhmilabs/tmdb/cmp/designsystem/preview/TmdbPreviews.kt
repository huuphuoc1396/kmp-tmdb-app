package com.banhmilabs.tmdb.cmp.designsystem.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.cmp.designsystem.TmdbTheme

@Preview(name = "English", locale = "en", device = Devices.PHONE)
@Preview(name = "Português (Brasil)", locale = "pt-rBR", device = Devices.PHONE)
@Preview(name = "Tiếng Việt", locale = "vi", device = Devices.PHONE)
@Preview(name = "Large text", locale = "en", fontScale = 2f, device = Devices.PHONE)
annotation class TmdbPreviews

@Preview(name = "English", locale = "en")
@Preview(name = "Português (Brasil)", locale = "pt-rBR")
@Preview(name = "Tiếng Việt", locale = "vi")
@Preview(name = "Large text", locale = "en", fontScale = 2f)
annotation class TmdbComponentPreviews

@Composable
fun TmdbPreview(content: @Composable () -> Unit) {
    TmdbTheme {
        Surface(color = MaterialTheme.colorScheme.surface, content = content)
    }
}
