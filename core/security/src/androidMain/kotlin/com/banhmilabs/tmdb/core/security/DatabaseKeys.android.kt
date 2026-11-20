package com.banhmilabs.tmdb.core.security

import android.content.Context
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.security.SecureRandom

/** Android: keys in `noBackupFilesDir`, so they are never part of a backup, encrypted with the Keystore. */
fun DatabaseKeys(context: Context): DatabaseKeys = EnvelopeDatabaseKeys(
    directory = context.noBackupFilesDir.toOkioPath() / "database-keys",
    fileSystem = FileSystem.SYSTEM,
    cipher = masterKeyCipher(DATABASE_MASTER_KEY_ALIAS),
    generateKey = { ByteArray(DATABASE_KEY_SIZE_BYTES).also(SecureRandom()::nextBytes) },
)
