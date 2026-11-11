package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.CorruptionException
import com.banhmilabs.tmdb.core.security.masterKeyCipher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EncryptedJsonSerializerTest {
    private val cipher = masterKeyCipher("EncryptedJsonSerializerTest")
    private val serializer = EncryptedJsonSerializer(Stored.serializer(), Stored(), cipher)

    @Test
    fun writeThenReadRestoresValue() = runTest {
        val buffer = Buffer()
        serializer.writeTo(Stored(flag = true, name = "secret"), buffer)

        assertEquals(Stored(flag = true, name = "secret"), serializer.readFrom(buffer))
    }

    @Test
    fun writtenBytesDoNotContainPlaintext() = runTest {
        val buffer = Buffer()
        serializer.writeTo(Stored(flag = true, name = "secret"), buffer)

        assertFalse(buffer.readByteArray().decodeToString().contains("secret"))
    }

    @Test
    fun undecryptableBytesThrowCorruptionException() = runTest {
        assertFailsWith<CorruptionException> { serializer.readFrom(Buffer().write(ByteArray(64) { it.toByte() })) }
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

    @Serializable
    private data class Stored(val flag: Boolean = false, val name: String = "")
}
