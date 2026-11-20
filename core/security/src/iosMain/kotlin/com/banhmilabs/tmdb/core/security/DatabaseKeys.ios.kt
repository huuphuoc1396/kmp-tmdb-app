package com.banhmilabs.tmdb.core.security

/** iOS: databases are not encrypted yet, so there is no passphrase. */
fun DatabaseKeys(): DatabaseKeys = object : DatabaseKeys {
    override fun passphrase(name: String): ByteArray? = null
}
