package com.banhmilabs.tmdb.core.security

import okio.FileSystem
import okio.Path

/**
 * Keeps one random key per database in [directory], encrypted with [cipher]. A key file that can't be decrypted
 * (master key lost, file corrupt) is replaced by a new key, so the existing database can no longer be opened.
 */
internal class EnvelopeDatabaseKeys(
    private val directory: Path,
    private val fileSystem: FileSystem,
    private val cipher: MasterKeyCipher,
    private val generateKey: () -> ByteArray,
) : DatabaseKeys {
    override fun passphrase(name: String): ByteArray {
        val file = directory / "$name.key"
        if (fileSystem.exists(file)) {
            try {
                return cipher.decrypt(fileSystem.read(file) { readByteArray() })
            } catch (_: CipherException) {
                // Replaced by a new key below.
            }
        }
        val key = generateKey()
        val temporary = directory / "$name.key.tmp"
        fileSystem.createDirectories(directory)
        fileSystem.write(temporary) { write(cipher.encrypt(key)) }
        fileSystem.atomicMove(temporary, file)
        return key
    }
}
