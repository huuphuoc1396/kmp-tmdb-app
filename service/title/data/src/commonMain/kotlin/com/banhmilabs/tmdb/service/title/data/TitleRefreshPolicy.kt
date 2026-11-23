package com.banhmilabs.tmdb.service.title.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal object TitleRefreshPolicy {
    val LIST_TTL = 6.hours
    val DETAILS_TTL = 24.hours

    fun isStale(fetchedAt: Instant?, now: Instant, ttl: Duration): Boolean = fetchedAt == null || now - fetchedAt > ttl
}
