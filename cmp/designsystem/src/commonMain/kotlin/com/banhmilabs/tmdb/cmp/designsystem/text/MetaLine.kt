package com.banhmilabs.tmdb.cmp.designsystem.text

fun metaLine(year: Int?, genre: String?): String = listOfNotNull(year?.toString(), genre).joinToString(" · ")
