package com.banhmilabs.tmdb.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.SecItemDelete

@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteMasterKeyForTest(alias: String) {
    withKeychainQuery(alias) { query -> SecItemDelete(query) }
}
