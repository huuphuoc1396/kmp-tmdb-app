package com.banhmilabs.tmdb.service.title.data

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** How long cached title data stays fresh. */
internal object TitleRefreshPolicy {
    val HOME_LIST_TTL = 6.hours

    /** A list never fetched, or fetched more than [HOME_LIST_TTL] before [now], is stale. */
    fun isStale(fetchedAtMillis: Long?, now: Instant): Boolean =
        fetchedAtMillis == null || now - Instant.fromEpochMilliseconds(fetchedAtMillis) > HOME_LIST_TTL
}
