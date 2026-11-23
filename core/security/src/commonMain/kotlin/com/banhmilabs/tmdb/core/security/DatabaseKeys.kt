package com.banhmilabs.tmdb.core.security

interface DatabaseKeys {
    fun passphrase(name: String): ByteArray?
}

internal const val DATABASE_KEY_SIZE_BYTES = 32
internal const val DATABASE_MASTER_KEY_ALIAS = "database"
