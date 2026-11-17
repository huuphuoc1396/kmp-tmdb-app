package com.banhmilabs.tmdb.core.security

import java.security.KeyStore

internal fun deleteMasterKeyForTest(alias: String) {
    KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(alias)
}
