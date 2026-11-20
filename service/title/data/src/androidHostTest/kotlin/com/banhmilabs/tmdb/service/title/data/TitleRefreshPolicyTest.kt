package com.banhmilabs.tmdb.service.title.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TitleRefreshPolicyTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun neverFetchedIsStale() {
        assertTrue(TitleRefreshPolicy.isStale(null, now, TitleRefreshPolicy.LIST_TTL))
    }

    @Test
    fun sevenHoursOldIsStale() {
        assertTrue(TitleRefreshPolicy.isStale(now - 7.hours, now, TitleRefreshPolicy.LIST_TTL))
    }

    @Test
    fun oneHourOldIsFresh() {
        assertFalse(TitleRefreshPolicy.isStale(now - 1.hours, now, TitleRefreshPolicy.LIST_TTL))
    }

    @Test
    fun detailsAreFreshFor24Hours() {
        assertFalse(TitleRefreshPolicy.isStale(now - 23.hours, now, TitleRefreshPolicy.DETAILS_TTL))
    }

    @Test
    fun detailsAreStaleAfter24Hours() {
        assertTrue(TitleRefreshPolicy.isStale(now - 25.hours, now, TitleRefreshPolicy.DETAILS_TTL))
    }
}
