package com.banhmilabs.tmdb.core.security

interface MasterKeyCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(ciphertext: ByteArray): ByteArray
}

expect fun masterKeyCipher(alias: String): MasterKeyCipher

internal const val IV_SIZE_BYTES = 12
internal const val TAG_SIZE_BYTES = 16
internal const val TAG_SIZE_BITS = 128
internal const val KEY_SIZE_BITS = 256

internal fun requireCiphertextSize(ciphertext: ByteArray) {
    if (ciphertext.size < IV_SIZE_BYTES + TAG_SIZE_BYTES) throw CipherException("Ciphertext too short")
}
