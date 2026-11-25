package com.banhmilabs.tmdb.cmp.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing

@Composable
internal fun ColorsSection() {
    val colors = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to colors.primary,
        "onPrimary" to colors.onPrimary,
        "primaryContainer" to colors.primaryContainer,
        "secondary" to colors.secondary,
        "onSecondary" to colors.onSecondary,
        "secondaryContainer" to colors.secondaryContainer,
        "tertiary" to colors.tertiary,
        "tertiaryContainer" to colors.tertiaryContainer,
        "surface" to colors.surface,
        "onSurface" to colors.onSurface,
        "onSurfaceVariant" to colors.onSurfaceVariant,
        "surfaceContainerLowest" to colors.surfaceContainerLowest,
        "surfaceContainerLow" to colors.surfaceContainerLow,
        "surfaceContainer" to colors.surfaceContainer,
        "surfaceContainerHigh" to colors.surfaceContainerHigh,
        "surfaceContainerHighest" to colors.surfaceContainerHighest,
        "outline" to colors.outline,
        "outlineVariant" to colors.outlineVariant,
        "error" to colors.error,
        "errorContainer" to colors.errorContainer,
        "scrim" to colors.scrim,
    )
    CatalogSection(
        title = "Colors",
        usage = "color = MaterialTheme.colorScheme.<role>; never Color(0x…) in a screen.",
    ) {
        roles.forEach { (name, color) -> ColorRow(name = name, color = color) }
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(TmdbSize.touchTarget)
                .background(color, MaterialTheme.shapes.small)
                .border(TmdbSize.borderWidth, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        )
        Column(modifier = Modifier.padding(start = TmdbSpacing.sm)) {
            CatalogLabel(name)
            CatalogUsage("colorScheme.$name · #${color.toArgb().toUInt().toString(radix = 16).uppercase()}")
        }
    }
}

@Composable
internal fun TypographySection() {
    val type = MaterialTheme.typography
    val styles = listOf(
        "displayLarge" to type.displayLarge,
        "displayMedium" to type.displayMedium,
        "displaySmall" to type.displaySmall,
        "headlineLarge" to type.headlineLarge,
        "headlineMedium" to type.headlineMedium,
        "headlineSmall" to type.headlineSmall,
        "titleLarge" to type.titleLarge,
        "titleMedium" to type.titleMedium,
        "titleSmall" to type.titleSmall,
        "bodyLarge" to type.bodyLarge,
        "bodyMedium" to type.bodyMedium,
        "bodySmall" to type.bodySmall,
        "labelLarge" to type.labelLarge,
        "labelMedium" to type.labelMedium,
        "labelSmall" to type.labelSmall,
    )
    CatalogSection(
        title = "Typography",
        usage = "style = MaterialTheme.typography.<style>; never fontSize = … in a screen.",
    ) {
        styles.forEach { (name, style) -> TypeRow(name = name, style = style) }
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle) {
    Column {
        Text(text = CATALOG_SAMPLE_TEXT, color = MaterialTheme.colorScheme.onSurface, style = style)
        CatalogUsage("typography.$name · ${style.fontSize} / ${style.lineHeight} · ${style.fontWeight?.weight}")
    }
}

@Composable
internal fun ShapesSection() {
    val shapes = MaterialTheme.shapes
    val all = listOf(
        "extraSmall" to shapes.extraSmall,
        "small" to shapes.small,
        "medium" to shapes.medium,
        "large" to shapes.large,
        "extraLarge" to shapes.extraLarge,
    )
    CatalogSection(
        title = "Shapes",
        usage = "shape = MaterialTheme.shapes.<size>, or CircleShape for pills and round buttons.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.sm)) {
            all.forEach { (name, shape) -> ShapeSample(name = name, shape = shape) }
        }
    }
}

@Composable
private fun ShapeSample(name: String, shape: Shape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(TmdbSpacing.xxxl + TmdbSpacing.xs)
                .background(MaterialTheme.colorScheme.primaryContainer, shape),
        )
        CatalogUsage(name)
    }
}

@Composable
internal fun SpacingSection() {
    val steps = listOf(
        "xxs" to TmdbSpacing.xxs,
        "xs" to TmdbSpacing.xs,
        "sm" to TmdbSpacing.sm,
        "md" to TmdbSpacing.md,
        "lg" to TmdbSpacing.lg,
        "xl" to TmdbSpacing.xl,
        "xxl" to TmdbSpacing.xxl,
        "xxxl" to TmdbSpacing.xxxl,
    )
    CatalogSection(
        title = "Spacing",
        usage = "Modifier.padding(TmdbSpacing.md), Arrangement.spacedBy(TmdbSpacing.sm).",
    ) {
        steps.forEach { (name, value) -> MeasureRow(name = "TmdbSpacing.$name", value = value) }
    }
}

@Composable
internal fun SizesSection() {
    val sizes = listOf(
        "touchTarget" to TmdbSize.touchTarget,
        "iconMedium" to TmdbSize.iconMedium,
        "iconSmall" to TmdbSize.iconSmall,
        "borderWidth" to TmdbSize.borderWidth,
    )
    CatalogSection(
        title = "Sizes",
        usage = "Modifier.size(TmdbSize.iconMedium); every tappable element is at least TmdbSize.touchTarget.",
    ) {
        sizes.forEach { (name, value) -> MeasureRow(name = "TmdbSize.$name", value = value) }
    }
}

@Composable
private fun MeasureRow(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(value)
                .height(TmdbSpacing.md)
                .background(MaterialTheme.colorScheme.secondary),
        )
        CatalogUsage(text = "$name · $value", modifier = Modifier.padding(start = TmdbSpacing.sm))
    }
}
