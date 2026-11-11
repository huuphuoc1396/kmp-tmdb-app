package com.banhmilabs.tmdb.core.security

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import java.security.GeneralSecurityException
import java.util.concurrent.ConcurrentHashMap

/** In-memory keys, one per alias, for the lifetime of the process (the JVM target is for tests only). */
internal val inMemoryKeys = ConcurrentHashMap<String, AES.GCM.Key>()

actual fun masterKeyCipher(alias: String): MasterKeyCipher = InMemoryKeyCipher(
    inMemoryKeys.computeIfAbsent(alias) {
        CryptographyProvider.Default.get(AES.GCM).keyGenerator(AES.Key.Size.B256).generateKeyBlocking()
    },
)

/** AES-GCM with an in-memory key through `cryptography-kotlin`. */
internal class InMemoryKeyCipher(key: AES.GCM.Key) : MasterKeyCipher {
    private val cipher = key.cipher()

    override fun encrypt(plaintext: ByteArray): ByteArray = cipher.encryptBlocking(plaintext)

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        requireCiphertextSize(ciphertext)
        return try {
            cipher.decryptBlocking(ciphertext)
        } catch (e: IllegalStateException) {
            throw CipherException("Decryption failed", e)
        } catch (e: GeneralSecurityException) {
            throw CipherException("Decryption failed", e)
        }
    }
}
