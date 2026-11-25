package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.cmp.designsystem.text.NameText
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.formatReleaseDate
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.formatRuntime
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.ic_clock
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.nameWithYear
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.previewDetails
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun DetailsHeader(details: TitleDetails, language: Language, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        RatingRing(percent = details.ratingPercent)
        Spacer(modifier = Modifier.width(TmdbSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            NameText(
                text = nameWithYear(details.name, details.releaseYear),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            val date = details.releaseDate?.let { formatReleaseDate(it, language) }
            val runtime = details.runtimeMinutes?.let(::formatRuntime)
            if (date != null || runtime != null) {
                Spacer(modifier = Modifier.height(TmdbSpacing.xxs))
                DateLine(date = date, runtime = runtime)
            }
        }
    }
}

@Composable
private fun DateLine(date: String?, runtime: String?) {
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
        if (date != null) DateLineText(text = date)
        if (runtime != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (date != null) {
                    Spacer(modifier = Modifier.width(TmdbSpacing.xxs))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(TmdbSpacing.xs))
                }
                Image(
                    painter = painterResource(Res.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(TmdbSize.iconSmall),
                )
                Spacer(modifier = Modifier.width(TmdbSpacing.xs))
                DateLineText(text = runtime)
            }
        }
    }
}

@Composable
private fun DateLineText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@TmdbComponentPreviews
@Composable
private fun DetailsHeaderPreview() {
    TmdbPreview {
        DetailsHeader(
            details = previewDetails,
            language = Language.fromLocaleTag(Locale.current.toLanguageTag()),
            modifier = Modifier.padding(TmdbSpacing.md),
        )
    }
}

@Preview(name = "Unknown date, runtime and rating")
@Composable
private fun DetailsHeaderUnknownPreview() {
    TmdbPreview {
        DetailsHeader(
            details = previewDetails.copy(releaseDate = null, runtimeMinutes = null, ratingPercent = null),
            language = Language.EN_US,
            modifier = Modifier.padding(TmdbSpacing.md),
        )
    }
}
