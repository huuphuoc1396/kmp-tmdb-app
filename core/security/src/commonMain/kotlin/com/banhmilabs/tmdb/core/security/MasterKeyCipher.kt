package com.banhmilabs.tmdb.core.security

/** AES-256-GCM with a per-device master key. Output of [encrypt]: iv (12 bytes) || ciphertext || tag (16 bytes). */
interface MasterKeyCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    /** @throws CipherException if the data was tampered with, is too short, or the key doesn't match. */
    fun decrypt(ciphertext: ByteArray): ByteArray
}

/** Returns the cipher for [alias], creating and storing the key on first use. */
expect fun masterKeyCipher(alias: String): MasterKeyCipher

internal const val IV_SIZE_BYTES = 12
internal const val TAG_SIZE_BYTES = 16
internal const val TAG_SIZE_BITS = 128
internal const val KEY_SIZE_BITS = 256

internal fun requireCiphertextSize(ciphertext: ByteArray) {
    if (ciphertext.size < IV_SIZE_BYTES + TAG_SIZE_BYTES) throw CipherException("Ciphertext too short")
}
