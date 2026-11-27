package com.banhmilabs.tmdb.nativeapp.designsystem.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbTheme

@Preview(name = "English", locale = "en", device = Devices.PHONE)
@Preview(name = "Português (Brasil)", locale = "pt-rBR", device = Devices.PHONE)
@Preview(name = "Tiếng Việt", locale = "vi", device = Devices.PHONE)
@Preview(name = "Large text", locale = "en", fontScale = 2f, device = Devices.PHONE)
internal annotation class TmdbPreviews

@Preview(name = "English", locale = "en")
@Preview(name = "Português (Brasil)", locale = "pt-rBR")
@Preview(name = "Tiếng Việt", locale = "vi")
@Preview(name = "Large text", locale = "en", fontScale = 2f)
internal annotation class TmdbComponentPreviews

@Composable
internal fun TmdbPreview(content: @Composable () -> Unit) {
    TmdbTheme {
        Surface(color = MaterialTheme.colorScheme.surface, content = content)
    }
}
