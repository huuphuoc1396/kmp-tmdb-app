package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamespaceTest {
    @Test
    fun coreModule() {
        assertEquals("com.banhmilabs.tmdb.core.common", namespaceOf(":core:common"))
    }

    @Test
    fun strippedDashes() {
        assertEquals("com.banhmilabs.tmdb.service.watchlist.domain", namespaceOf(":service:watch-list:domain"))
    }

    @Test
    fun nativeAppSegmentIsAliased() {
        assertEquals("com.banhmilabs.tmdb.apps.nativeapp.designsystem", namespaceOf(":apps:native:designsystem"))
        assertEquals("nativeapp", packageSegment("native"))
    }

    @Test
    fun cmpUnchanged() {
        assertEquals("com.banhmilabs.tmdb.apps.cmp.composeApp", namespaceOf(":apps:cmp:composeApp"))
    }
}
