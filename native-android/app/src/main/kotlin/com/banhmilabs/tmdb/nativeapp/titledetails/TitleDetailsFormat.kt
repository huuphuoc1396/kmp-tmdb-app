package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private const val MINUTES_PER_HOUR = 60

internal fun formatReleaseDate(date: LocalDate, language: Language): String {
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    val year = date.year.toString().padStart(4, '0')
    return when (language) {
        Language.PT_BR -> "$day/$month/$year"
        Language.EN_US -> "$month/$day/$year"
    }
}

internal fun formatRuntime(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

internal fun nameWithYear(name: String, year: Int?): String = if (year == null) name else "$name\u00A0($year)"
