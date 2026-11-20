package com.banhmilabs.tmdb.core.security

/** Passphrases for local databases; the same name always gets the same passphrase on this device. */
interface DatabaseKeys {
    /** Passphrase for database [name], created on first use; null when the platform has no encryption yet. */
    fun passphrase(name: String): ByteArray?
}

internal const val DATABASE_KEY_SIZE_BYTES = 32
internal const val DATABASE_MASTER_KEY_ALIAS = "database"
