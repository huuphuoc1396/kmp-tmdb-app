package com.banhmilabs.tmdb.nativeapp.titledetails.component

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingRingFontScaleTest {

    @Test
    fun smallScalesPassThrough() {
        assertEquals(0.85f, ringFontScale(0.85f))
        assertEquals(1f, ringFontScale(1f))
    }

    @Test
    fun largeScalesAreCappedAtTheMaximum() {
        assertEquals(MAX_RING_FONT_SCALE, ringFontScale(2f))
    }

    @Test
    fun theMaximumItselfPassesThrough() {
        assertEquals(MAX_RING_FONT_SCALE, ringFontScale(MAX_RING_FONT_SCALE))
    }
}
