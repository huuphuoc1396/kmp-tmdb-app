package com.banhmilabs.tmdb.core.security

import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun DatabaseKeys(): DatabaseKeys {
    val directory = applicationSupportDirectory() / "database-keys"
    excludeFromBackup(directory.toString())
    return EnvelopeDatabaseKeys(
        directory = directory,
        fileSystem = FileSystem.SYSTEM,
        cipher = masterKeyCipher(DATABASE_MASTER_KEY_ALIAS),
        generateKey = { CryptographyRandom.nextBytes(DATABASE_KEY_SIZE_BYTES) },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): Path {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return checkNotNull(url?.path) { "Application Support directory unavailable" }.toPath()
}
