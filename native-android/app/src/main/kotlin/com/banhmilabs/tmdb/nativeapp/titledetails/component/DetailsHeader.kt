package com.banhmilabs.tmdb.nativeapp.titledetails.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.titledetails.DetailsColors
import com.banhmilabs.tmdb.nativeapp.titledetails.formatReleaseDate
import com.banhmilabs.tmdb.nativeapp.titledetails.formatRuntime
import com.banhmilabs.tmdb.nativeapp.titledetails.nameWithYear
import com.banhmilabs.tmdb.service.title.domain.TitleDetails

private val RingToName = 16.dp
private val NameToDateLine = 5.dp
private val DateToDot = 3.dp
private val DotSize = 4.dp
private val DotToClock = 6.dp
private val ClockSize = 19.dp
private val ClockToRuntime = 6.dp

private val NameStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
private val DateLineStyle = TextStyle(color = DetailsColors.DateLine, fontSize = 16.sp)

/** The rating ring next to "Name (year)" and the release date • runtime line. */
@Composable
internal fun DetailsHeader(details: TitleDetails, language: Language, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        RatingRing(percent = details.ratingPercent)
        Spacer(modifier = Modifier.width(RingToName))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameWithYear(details.name, details.releaseYear),
                modifier = Modifier.semantics { heading() },
                style = NameStyle,
            )
            val date = details.releaseDate?.let { formatReleaseDate(it, language) }
            val runtime = details.runtimeMinutes?.let(::formatRuntime)
            if (date != null || runtime != null) {
                Spacer(modifier = Modifier.height(NameToDateLine))
                DateLine(date = date, runtime = runtime)
            }
        }
    }
}

/** Date and runtime; a missing part is left out together with its separator. */
@Composable
private fun DateLine(date: String?, runtime: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (date != null) Text(text = date, style = DateLineStyle)
        if (date != null && runtime != null) {
            Spacer(modifier = Modifier.width(DateToDot))
            Box(modifier = Modifier.size(DotSize).background(DetailsColors.DateLine, CircleShape))
            Spacer(modifier = Modifier.width(DotToClock))
        }
        if (runtime != null) {
            Image(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(ClockSize),
            )
            Spacer(modifier = Modifier.width(ClockToRuntime))
            Text(text = runtime, style = DateLineStyle)
        }
    }
}
