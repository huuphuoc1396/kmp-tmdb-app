package com.banhmilabs.tmdb.cmp.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TmdbSpacingTest {

    private val steps = with(TmdbSpacing) { listOf(xxs, xs, sm, md, lg, xl, xxl, xxxl) }

    @Test
    fun everyStepIsAMultipleOfFour() {
        steps.forEach { step -> assertEquals(0f, step.value % 4f, "$step is not on the 4dp grid") }
    }

    @Test
    fun stepsGrowFromXxsToXxxl() {
        steps.zipWithNext().forEach { (smaller, larger) -> assertTrue(smaller < larger, "$smaller >= $larger") }
    }
}
