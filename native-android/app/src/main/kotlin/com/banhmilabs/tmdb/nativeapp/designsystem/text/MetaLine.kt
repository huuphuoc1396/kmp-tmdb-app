package com.banhmilabs.tmdb.nativeapp.designsystem.text

internal fun metaLine(year: Int?, genre: String?): String = listOfNotNull(year?.toString(), genre).joinToString(" · ")
