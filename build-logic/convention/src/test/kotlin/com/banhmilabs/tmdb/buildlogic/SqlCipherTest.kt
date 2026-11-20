package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun zipWithExpectedChecksumPasses() {
        // SHA-256 of "abc".
        checkSqlCipherZip("abc".toByteArray(), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test
    fun lfsPointerFailsWithHint() {
        val pointer = "version https://git-lfs.github.com/spec/v1\noid sha256:00\nsize 1\n".toByteArray()
        val error = assertThrows<GradleException> { checkSqlCipherZip(pointer, "00") }
        assertTrue(error.message!!.contains("git lfs pull"))
    }
}
