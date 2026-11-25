package com.banhmilabs.tmdb.nativeapp.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview

@Preview(name = "Design system catalog", widthDp = 411, heightDp = 7200)
@Composable
private fun DesignSystemCatalogPreview() {
    TmdbPreview {
        DesignSystemCatalog()
    }
}

@Composable
private fun DesignSystemCatalog() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(TmdbSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TmdbSpacing.xxl),
    ) {
        Text(
            text = "TMDB design system",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
        CatalogUsage("Wrap every screen in TmdbTheme { }; read tokens from MaterialTheme, TmdbSpacing and TmdbSize.")
        ColorsSection()
        TypographySection()
        ShapesSection()
        SpacingSection()
        SizesSection()
        TextSection()
        ComponentsSection()
    }
}

@Composable
internal fun CatalogSection(title: String, usage: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TmdbSpacing.md)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
        CatalogUsage(usage)
        content()
    }
}

@Composable
internal fun CatalogEntry(name: String, usage: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TmdbSpacing.xs)) {
        Text(text = name, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleMedium)
        CatalogUsage(usage)
        content()
    }
}

@Composable
internal fun CatalogLabel(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun CatalogUsage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    )
}

internal const val CATALOG_SAMPLE_TEXT = "The Wild Robot"
