package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.CorruptionException
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncryptedJsonSerializerTest {
    private val cipher = PassThroughCipher()
    private val serializer = EncryptedJsonSerializer(Stored.serializer(), Stored(), cipher)

    @Test
    fun writeThenReadRestoresValue() = runTest {
        val buffer = Buffer()
        serializer.writeTo(Stored(flag = true, name = "secret"), buffer)

        assertEquals(Stored(flag = true, name = "secret"), serializer.readFrom(buffer))
    }

    @Test
    fun undecryptableBytesThrowCorruptionException() = runTest {
        val failingCipher = PassThroughCipher(decryptFailure = CipherException("Tag mismatch"))
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

    private class PassThroughCipher(
        private val encryptFailure: CipherException? = null,
        private val decryptFailure: CipherException? = null,
    ) : MasterKeyCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray {
            encryptFailure?.let { throw it }
            return plaintext
        }

        override fun decrypt(ciphertext: ByteArray): ByteArray {
            decryptFailure?.let { throw it }
            return ciphertext
        }
    }

    @Serializable
    private data class Stored(val flag: Boolean = false, val name: String = "")
}
