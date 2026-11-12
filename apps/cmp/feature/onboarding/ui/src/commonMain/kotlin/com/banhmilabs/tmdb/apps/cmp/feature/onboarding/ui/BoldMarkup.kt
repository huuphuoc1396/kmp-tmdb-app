package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private const val BOLD_OPEN = "<b>"
private const val BOLD_CLOSE = "</b>"

/**
 * `"All about <b>movies</b>"` -> "All about movies" with "movies" bold. Only `<b>` is supported; an unclosed tag
 * runs to the end. (`AnnotatedString.fromHtml` exists on Android only.)
 */
internal fun boldMarkupToAnnotatedString(markup: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < markup.length) {
        val open = markup.indexOf(BOLD_OPEN, index)
        if (open < 0) {
            append(markup.substring(index))
            break
        }
        append(markup.substring(index, open))
        val start = open + BOLD_OPEN.length
        val close = markup.indexOf(BOLD_CLOSE, start).takeIf { it >= 0 } ?: markup.length
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(markup.substring(start, close)) }
        index = minOf(close + BOLD_CLOSE.length, markup.length)
    }
}
