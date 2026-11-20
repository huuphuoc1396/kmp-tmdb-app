package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SqlCipherTest {
    @Test
    fun slicesMatchIosTargets() {
        assertEquals("ios-arm64", sqlCipherSlice("iosArm64"))
        assertEquals("ios-arm64_x86_64-simulator", sqlCipherSlice("iosSimulatorArm64"))
    }

    @Test
    fun unknownTargetHasNoSlice() {
        assertThrows<IllegalStateException> { sqlCipherSlice("macosArm64") }
    }
}
