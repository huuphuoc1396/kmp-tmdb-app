package com.banhmilabs.tmdb.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual fun masterKeyCipher(alias: String): MasterKeyCipher = AndroidKeystoreCipher(alias)

internal const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** AES-GCM with a non-exportable Android Keystore key; the key never leaves the Keystore. */
internal class AndroidKeystoreCipher(private val alias: String) : MasterKeyCipher {
    private val key: SecretKey by lazy { loadOrCreateKey() }

    override fun encrypt(plaintext: ByteArray): ByteArray = try {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        cipher.iv + cipher.doFinal(plaintext)
    } catch (e: GeneralSecurityException) {
        throw CipherException("Encryption failed", e)
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        requireCiphertextSize(ciphertext)
        return try {
            val spec = GCMParameterSpec(TAG_SIZE_BITS, ciphertext, 0, IV_SIZE_BYTES)
            Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.DECRYPT_MODE, key, spec) }
                .doFinal(ciphertext, IV_SIZE_BYTES, ciphertext.size - IV_SIZE_BYTES)
        } catch (e: GeneralSecurityException) {
            throw CipherException("Decryption failed", e)
        }
    }

    private fun loadOrCreateKey(): SecretKey = try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as SecretKey?) ?: generateKey()
    } catch (e: GeneralSecurityException) {
        throw CipherException("Keystore error", e)
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
