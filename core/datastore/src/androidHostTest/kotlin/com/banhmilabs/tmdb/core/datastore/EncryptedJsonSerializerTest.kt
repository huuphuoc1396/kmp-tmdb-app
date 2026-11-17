package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.CorruptionException
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncryptedJsonSerializerTest {
    private val cipher = passThroughCipher()
    private val serializer = EncryptedJsonSerializer(Stored.serializer(), Stored(), cipher)

    @Test
    fun writeThenReadRestoresValue() = runTest {
        val buffer = Buffer()
        serializer.writeTo(Stored(flag = true, name = "secret"), buffer)

        assertEquals(Stored(flag = true, name = "secret"), serializer.readFrom(buffer))
    }

    @Test
    fun undecryptableBytesThrowCorruptionException() = runTest {
        val failingCipher = mockk<MasterKeyCipher> {
            every { decrypt(any()) } throws CipherException("Tag mismatch")
        }
        val failingSerializer = EncryptedJsonSerializer(Stored.serializer(), Stored(), failingCipher)

        assertFailsWith<CorruptionException> {
            failingSerializer.readFrom(Buffer().write(ByteArray(64) { it.toByte() }))
        }
    }

    @Test
    fun invalidJsonThrowsCorruptionException() = runTest {
        val buffer = Buffer().write(cipher.encrypt("not json".encodeToByteArray()))

        assertFailsWith<CorruptionException> { serializer.readFrom(buffer) }
    }

    @Test
    fun unknownKeysAreIgnored() = runTest {
        val buffer = Buffer().write(cipher.encrypt("""{"flag":true,"addedLater":1}""".encodeToByteArray()))

        assertEquals(Stored(flag = true), serializer.readFrom(buffer))
    }

    private fun passThroughCipher(): MasterKeyCipher = mockk {
        every { encrypt(any()) } answers { firstArg() }
        every { decrypt(any()) } answers { firstArg() }
    }

    @Serializable
    private data class Stored(val flag: Boolean = false, val name: String = "")
}
