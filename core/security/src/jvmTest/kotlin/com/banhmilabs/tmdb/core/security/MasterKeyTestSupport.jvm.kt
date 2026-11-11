package com.banhmilabs.tmdb.core.security

internal actual fun deleteMasterKeyForTest(alias: String) {
    inMemoryKeys.remove(alias)
}
