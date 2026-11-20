package com.banhmilabs.tmdb.service.title.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** How long cached title data stays fresh. */
internal object TitleRefreshPolicy {
    val LIST_TTL = 6.hours
    val DETAILS_TTL = 24.hours

    /** Data never fetched, or fetched more than [ttl] before [now], is stale. */
    fun isStale(fetchedAt: Instant?, now: Instant, ttl: Duration): Boolean = fetchedAt == null || now - fetchedAt > ttl
}
