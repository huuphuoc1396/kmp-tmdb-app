package com.banhmilabs.tmdb.cmp.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.FavoriteHeart
import com.banhmilabs.tmdb.cmp.designsystem.component.PersonIcon
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRow
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterRowDefaults
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.cmp.designsystem.component.RetryMessage
import com.banhmilabs.tmdb.cmp.designsystem.component.removeFavoriteLabel
import com.banhmilabs.tmdb.cmp.designsystem.text.NameText
import com.banhmilabs.tmdb.cmp.designsystem.text.metaLine
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType

private val catalogTitles = listOf("Dune: Part Two", "The Wild Robot", "Kingdom of the Planet of the Apes")
    .mapIndexed { index, name -> Title(TitleId(index + 1L, TitleType.MOVIE), name, posterUrl = null) }

@Composable
internal fun TextSection() {
    CatalogSection(title = "Text", usage = "Helpers in designsystem.text.") {
        CatalogEntry(
            name = "NameText",
            usage = "NameText(text, style, color, maxLines = 2): stops at a word wider than its line, " +
                "instead of breaking it.",
        ) {
            NameText(
                text = "Supercalifragilisticexpialidocious Adventures",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(PosterRowDefaults.PosterWidth),
                maxLines = 2,
            )
        }
        CatalogEntry(name = "metaLine", usage = "metaLine(year, genre): \"year · genre\", unknown parts left out.") {
            CatalogLabel(metaLine(year = 2024, genre = "Animation"))
            CatalogLabel(metaLine(year = null, genre = "Animation"))
        }
    }
}

@Composable
internal fun ComponentsSection() {
    CatalogSection(title = "Components", usage = "Reusable pieces in designsystem.component.") {
        CatalogEntry(name = "FavoriteHeart", usage = "FavoriteHeart(isFavorite): filled = favorite, outline = not.") {
            Row(horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.md)) {
                FavoriteHeart(isFavorite = true)
                FavoriteHeart(isFavorite = false)
            }
        }
        CatalogEntry(name = "PersonIcon", usage = "PersonIcon(): placeholder for a missing cast photo.") {
            PersonIcon()
        }
        CatalogEntry(
            name = "PosterTile",
            usage = "PosterTile(name, posterUrl): poster image, or the name while none.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.md)) {
                PosterTile(
                    name = CATALOG_SAMPLE_TEXT,
                    posterUrl = null,
                    modifier = Modifier.width(PosterRowDefaults.PosterWidth),
                )
                PosterTile(name = null, posterUrl = null, modifier = Modifier.width(PosterRowDefaults.PosterWidth))
            }
        }
        CatalogEntry(
            name = "PosterRow",
            usage = "PosterRow(titles, onTitleClick, contentPadding): a row of PosterRowDefaults 124×188 posters.",
        ) {
            PosterRow(titles = catalogTitles, onTitleClick = {}, contentPadding = PaddingValues(end = TmdbSpacing.md))
        }
        CatalogEntry(
            name = "RetryMessage",
            usage = "RetryMessage(message, retryLabel, onRetry): onRetry = null hides it.",
        ) {
            RetryMessage(message = "Couldn't load this list.", retryLabel = "Try again", onRetry = {})
        }
        CatalogEntry(
            name = "RemoveFavoriteDialog",
            usage = "RemoveFavoriteDialog(titleName, onConfirm, onDismiss): confirm before removing a favorite; " +
                "label the heart button with removeFavoriteLabel() (\"${removeFavoriteLabel()}\").",
        ) {}
    }
}
