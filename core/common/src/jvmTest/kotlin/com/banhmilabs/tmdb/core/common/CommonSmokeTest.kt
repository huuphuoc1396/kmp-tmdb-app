package com.banhmilabs.tmdb.core.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonSmokeTest {
    @Test
    fun testSourceSetRuns() {
        assertTrue(true, "Placeholder: replace when the module gets production code")
    }

    @Test
    fun mockkCreatesMocks() {
        val greeter = mockk<Greeter>()
        every { greeter.greet() } returns "hi"

        assertEquals("hi", greeter.greet())
        verify { greeter.greet() }
    }

    private fun interface Greeter {
        fun greet(): String
    }
}
