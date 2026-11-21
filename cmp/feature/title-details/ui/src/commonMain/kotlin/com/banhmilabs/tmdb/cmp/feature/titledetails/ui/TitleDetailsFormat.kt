package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import com.banhmilabs.tmdb.core.model.Language
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private const val MINUTES_PER_HOUR = 60

/** The release date as the app language writes it: `dd/MM/yyyy` in Portuguese, `MM/dd/yyyy` in English. */
internal fun formatReleaseDate(date: LocalDate, language: Language): String {
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    val year = date.year.toString().padStart(4, '0')
    return when (language) {
        Language.PT_BR -> "$day/$month/$year"
        Language.EN_US -> "$month/$day/$year"
    }
}

/** A runtime as hours and minutes: `1h 53m`, `2h`, `45m`. */
internal fun formatRuntime(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/**
 * The name followed by the year in parentheses, or the name alone when the year is unknown. A non-breaking space
 * keeps the year on the name's last line when it wraps.
 */
internal fun nameWithYear(name: String, year: Int?): String = if (year == null) name else "$name\u00A0($year)"
