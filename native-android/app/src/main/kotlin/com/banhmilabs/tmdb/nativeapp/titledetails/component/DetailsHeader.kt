package com.banhmilabs.tmdb.nativeapp.titledetails.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSize
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.text.NameText
import com.banhmilabs.tmdb.nativeapp.titledetails.formatReleaseDate
import com.banhmilabs.tmdb.nativeapp.titledetails.formatRuntime
import com.banhmilabs.tmdb.nativeapp.titledetails.nameWithYear
import com.banhmilabs.tmdb.service.title.domain.TitleDetails

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
                    painter = painterResource(R.drawable.ic_clock),
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
