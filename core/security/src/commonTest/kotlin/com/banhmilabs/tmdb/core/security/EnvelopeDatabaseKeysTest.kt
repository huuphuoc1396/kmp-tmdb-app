package com.banhmilabs.tmdb.core.security

import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EnvelopeDatabaseKeysTest {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "database-keys-${Random.nextLong()}"
    private val cipher = ReversingCipher()
    private var generatedKeys = 0

    @AfterTest
    fun deleteFiles() {
        fileSystem.deleteRecursively(directory)
    }

    @Test
    fun createsKeyOnFirstCall() {
        assertContentEquals(key(1), keys().passphrase(NAME))
    }

    @Test
    fun returnsSameKeyOnLaterCalls() {
        val first = keys().passphrase(NAME)

        val second = keys().passphrase(NAME)

        assertContentEquals(first, second)
        assertEquals(1, generatedKeys)
    }

    @Test
    fun storesOnlyEncryptedKey() {
        val key = keys().passphrase(NAME)

        val stored = fileSystem.read(directory / "$NAME.key") { readByteArray() }

        assertContentEquals(key.reversedArray(), stored)
        assertFalse(stored.contentEquals(key))
    }

    @Test
    fun replacesKeyThatCannotBeDecrypted() {
        val first = keys().passphrase(NAME)
        cipher.decryptFailure = CipherException("Master key lost")

        val second = keys().passphrase(NAME)

        assertFalse(second.contentEquals(first))
        assertContentEquals(second.reversedArray(), fileSystem.read(directory / "$NAME.key") { readByteArray() })
    }

    @Test
    fun keepsSeparateKeysPerDatabase() {
        val keys = keys()

        val a = keys.passphrase("a.db")
        val b = keys.passphrase("b.db")

        assertFalse(a.contentEquals(b))
        assertContentEquals(a, keys.passphrase("a.db"))
        assertContentEquals(b, keys.passphrase("b.db"))
    }

    private fun keys() = EnvelopeDatabaseKeys(directory, fileSystem, cipher) { key(++generatedKeys) }

    private fun key(seed: Int) = Random(seed).nextBytes(DATABASE_KEY_SIZE_BYTES)

    private class ReversingCipher : MasterKeyCipher {
        var decryptFailure: CipherException? = null

        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.reversedArray()

        override fun decrypt(ciphertext: ByteArray): ByteArray {
            decryptFailure?.let { throw it }
            return ciphertext.reversedArray()
        }
    }

    private companion object {
        const val NAME = "title.db"
    }
}
