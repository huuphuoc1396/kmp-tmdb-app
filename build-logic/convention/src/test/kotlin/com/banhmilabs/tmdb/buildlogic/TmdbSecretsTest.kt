package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TmdbSecretsTest {
    private val salt = ByteArray(32) { (it * 37 + 11).toByte() }

    @Test
    fun obfuscateThenXorWithSaltRestoresToken() {
        assertEquals(TOKEN, deobfuscate(obfuscate(TOKEN, salt)))
    }

    @Test
    fun obfuscatedBytesDifferFromPlainToken() {
        assertFalse(obfuscate(TOKEN, salt).contentEquals(TOKEN.encodeToByteArray()))
    }

    @Test
    fun emptyTokenGivesEmptyData() {
        assertArrayEquals(ByteArray(0), obfuscate("", salt))
    }

    @Test
    fun renderedSourceDeclaresInternalObjectInNetworkPackage() {
        val source = renderSecretsSource(obfuscate(TOKEN, salt), salt)

        assertTrue(source.contains("package com.banhmilabs.tmdb.core.network\n"), source)
        assertTrue(source.contains("internal object TmdbSecrets {"), source)
        assertTrue(source.contains("val accessToken: String"), source)
    }

    @Test
    fun renderedSourceDoesNotContainPlainToken() {
        val source = renderSecretsSource(obfuscate(TOKEN, salt), salt)

        assertFalse(source.contains(TOKEN))
        assertFalse(source.contains(TOKEN.encodeToByteArray().joinToString()))
    }

    @Test
    fun nonAsciiTokenRoundTrips() {
        val token = "tökén-日本-✓"

        assertEquals(token, deobfuscate(obfuscate(token, salt)))
    }

    private fun deobfuscate(data: ByteArray): String =
        ByteArray(data.size) { (data[it].toInt() xor salt[it % salt.size].toInt()).toByte() }.decodeToString()

    private companion object {
        const val TOKEN = "eyJhbGciOiJIUzI1NiJ9.sample-token"
    }
}
