package com.banhmilabs.tmdb.core.security

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** Runs on an Android device (Keystore, app files). */
class DatabaseKeysDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-${Random.nextLong()}.db"
    private val keyFile = context.noBackupFilesDir.resolve("database-keys/$name.key")

    @AfterTest
    fun deleteKey() {
        keyFile.delete()
        deleteMasterKeyForTest(DATABASE_MASTER_KEY_ALIAS)
    }

    @Test
    fun passphraseIs32Bytes() {
        assertEquals(DATABASE_KEY_SIZE_BYTES, assertNotNull(DatabaseKeys(context).passphrase(name)).size)
    }

    @Test
    fun passphraseIsStableAcrossInstances() {
        val first = DatabaseKeys(context).passphrase(name)

        assertContentEquals(first, DatabaseKeys(context).passphrase(name))
    }

    @Test
    fun keyFileDoesNotContainPassphrase() {
        val passphrase = assertNotNull(DatabaseKeys(context).passphrase(name))

        val stored = keyFile.readBytes()

        assertFalse(stored.asList().windowed(passphrase.size).any { it == passphrase.asList() })
    }

    @Test
    fun lostMasterKeyGivesNewPassphrase() {
        val first = DatabaseKeys(context).passphrase(name)
        deleteMasterKeyForTest(DATABASE_MASTER_KEY_ALIAS)

        val second = DatabaseKeys(context).passphrase(name)

        assertFalse(assertNotNull(second).contentEquals(first))
    }
}
