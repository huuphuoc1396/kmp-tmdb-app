package com.banhmilabs.tmdb.core.security

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

actual fun masterKeyCipher(alias: String): MasterKeyCipher = KeychainCipher(alias)

internal const val KEYCHAIN_SERVICE = "com.banhmilabs.tmdb.core.security"

/** AES-GCM through `cryptography-kotlin` with a key stored in the Keychain, readable only on this device. */
internal class KeychainCipher(private val alias: String) : MasterKeyCipher {
    private val cipher by lazy {
        CryptographyProvider.Default.get(AES.GCM).keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, readKey() ?: createKey())
            .cipher()
    }

    override fun encrypt(plaintext: ByteArray): ByteArray = try {
        cipher.encryptBlocking(plaintext)
    } catch (e: IllegalStateException) {
        throw CipherException("Encryption failed", e)
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        requireCiphertextSize(ciphertext)
        return try {
            cipher.decryptBlocking(ciphertext)
        } catch (e: IllegalStateException) {
            throw CipherException("Decryption failed", e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readKey(): ByteArray? = withKeychainQuery(alias, kSecReturnData to kCFBooleanTrue) { query ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, result.ptr)) {
                errSecSuccess -> (CFBridgingRelease(result.value) as NSData).toByteArray()
                errSecItemNotFound -> null
                else -> throw CipherException("Keychain read failed: $status")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createKey(): ByteArray {
        val key = CryptographyProvider.Default.get(AES.GCM).keyGenerator(AES.Key.Size.B256).generateKeyBlocking()
            .encodeToByteArrayBlocking(AES.Key.Format.RAW)
        val data = CFBridgingRetain(key.toNSData())
        try {
            val status = withKeychainQuery(
                alias,
                kSecValueData to data,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            ) { query -> SecItemAdd(query, null) }
            if (status != errSecSuccess) throw CipherException("Keychain write failed: $status")
        } finally {
            CFRelease(data)
        }
        return key
    }
}

/** Runs [block] with a generic-password query for [alias] in [KEYCHAIN_SERVICE] plus [extra] entries. */
@OptIn(ExperimentalForeignApi::class)
internal fun <T> withKeychainQuery(
    alias: String,
    vararg extra: Pair<CFStringRef?, CFTypeRef?>,
    block: (CFDictionaryRef?) -> T,
): T {
    val service = CFBridgingRetain(KEYCHAIN_SERVICE)
    val account = CFBridgingRetain(alias)
    val query = CFDictionaryCreateMutable(
        null,
        0,
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    )
    try {
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, account)
        extra.forEach { (key, value) -> CFDictionaryAddValue(query, key, value) }
        return block(query)
    } finally {
        CFRelease(query)
        CFRelease(service)
        CFRelease(account)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.convert()) }

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
