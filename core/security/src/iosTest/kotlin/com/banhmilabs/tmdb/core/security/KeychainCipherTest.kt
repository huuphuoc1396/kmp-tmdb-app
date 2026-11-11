package com.banhmilabs.tmdb.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Security.SecItemCopyMatching
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecReturnAttributes
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class KeychainCipherTest {
    private val alias = "test-${Random.nextLong()}"

    @AfterTest
    fun deleteKey() {
        deleteMasterKeyForTest(alias)
    }

    @Test
    fun keyIsStoredThisDeviceOnly() {
        masterKeyCipher(alias).encrypt(byteArrayOf(1))

        val attributes = withKeychainQuery(alias, kSecReturnAttributes to kCFBooleanTrue) { query ->
            memScoped {
                val result = alloc<CFTypeRefVar>()
                assertEquals(errSecSuccess, SecItemCopyMatching(query, result.ptr))
                CFBridgingRelease(result.value) as Map<*, *>
            }
        }

        assertEquals(
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly.bridged(),
            attributes[kSecAttrAccessible.bridged()],
        )
    }

    private fun CFTypeRef?.bridged(): Any? = CFBridgingRelease(CFRetain(this))
}
