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
        assertTrue(TitleRefreshPolicy.isStale(null, now))
    }

    @Test
    fun sevenHoursOldIsStale() {
        assertTrue(TitleRefreshPolicy.isStale(now - 7.hours, now))
    }

    @Test
    fun oneHourOldIsFresh() {
        assertFalse(TitleRefreshPolicy.isStale(now - 1.hours, now))
    }
}
