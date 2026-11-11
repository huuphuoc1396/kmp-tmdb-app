package com.banhmilabs.tmdb.core.security

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/** Runs on JVM, on an Android device (Keystore) and on the iOS simulator (Keychain). */
class MasterKeyCipherTest {
    private val aliases = mutableListOf<String>()
    private val plaintext = "onboardingCompleted=true".encodeToByteArray()

    @AfterTest
    fun deleteKeys() {
        aliases.forEach(::deleteMasterKeyForTest)
    }

    @Test
    fun encryptThenDecryptReturnsPlaintext() {
        val cipher = masterKeyCipher(newAlias())

        assertContentEquals(plaintext, cipher.decrypt(cipher.encrypt(plaintext)))
    }

    @Test
    fun ciphertextDoesNotContainPlaintext() {
        val ciphertext = masterKeyCipher(newAlias()).encrypt(plaintext)

        assertFalse(ciphertext.decodeToString(throwOnInvalidSequence = false).contains("onboardingCompleted"))
    }

    @Test
    fun encryptingTwiceGivesDifferentOutput() {
        val cipher = masterKeyCipher(newAlias())

        assertFalse(cipher.encrypt(plaintext).contentEquals(cipher.encrypt(plaintext)))
    }

    @Test
    fun tamperedCiphertextThrowsCipherException() {
        val cipher = masterKeyCipher(newAlias())
        val ciphertext = cipher.encrypt(plaintext)
        ciphertext[ciphertext.lastIndex] = (ciphertext.last() + 1).toByte()

        assertFailsWith<CipherException> { cipher.decrypt(ciphertext) }
    }

    @Test
    fun tooShortInputThrowsCipherException() {
        assertFailsWith<CipherException> { masterKeyCipher(newAlias()).decrypt(ByteArray(IV_SIZE_BYTES)) }
    }

    @Test
    fun newInstanceWithSameAliasDecrypts() {
        val alias = newAlias()
        val ciphertext = masterKeyCipher(alias).encrypt(plaintext)

        assertContentEquals(plaintext, masterKeyCipher(alias).decrypt(ciphertext))
    }

    @Test
    fun differentAliasCannotDecrypt() {
        val ciphertext = masterKeyCipher(newAlias()).encrypt(plaintext)

        assertFailsWith<CipherException> { masterKeyCipher(newAlias()).decrypt(ciphertext) }
    }

    private fun newAlias(): String = "test-${Random.nextLong()}".also(aliases::add)
}
