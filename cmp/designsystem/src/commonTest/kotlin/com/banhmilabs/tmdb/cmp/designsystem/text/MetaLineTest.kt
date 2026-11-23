package com.banhmilabs.tmdb.cmp.designsystem.text

import kotlin.test.Test
import kotlin.test.assertEquals

class MetaLineTest {
    @Test
    fun yearAndGenreAreJoinedWithDot() {
        assertEquals("2021 · Drama", metaLine(2021, "Drama"))
    }

    @Test
    fun missingYearLeavesGenreOnly() {
        assertEquals("Drama", metaLine(null, "Drama"))
    }

    @Test
    fun missingGenreLeavesYearOnly() {
        assertEquals("2021", metaLine(2021, null))
    }

    @Test
    fun bothMissingIsEmpty() {
        assertEquals("", metaLine(null, null))
    }
}
