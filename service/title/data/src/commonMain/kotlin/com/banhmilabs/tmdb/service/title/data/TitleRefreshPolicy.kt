package com.banhmilabs.tmdb.service.title.data

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** How long cached title data stays fresh. */
internal object TitleRefreshPolicy {
    val LIST_TTL = 6.hours

    /** A list never fetched, or fetched more than [LIST_TTL] before [now], is stale. */
    fun isStale(fetchedAt: Instant?, now: Instant): Boolean = fetchedAt == null || now - fetchedAt > LIST_TTL
}
