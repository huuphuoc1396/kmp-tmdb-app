package com.banhmilabs.tmdb.core.database

import co.touchlab.sqliter.DatabaseFileContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.posix.memcpy
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseDriverFactoryTest {
    private val factory = DatabaseDriverFactory()
    private val name = "test-${Random.nextLong()}.db"
    private val passphrase = Random.nextBytes(32)

    @AfterTest
    fun deleteDatabase() {
        DatabaseFileContext.deleteDatabase(name)
    }

    @Test
    fun createsSchemaOnFirstOpen() {
        val driver = factory.create(TestSchema, name)

        assertTrue(driver.itemNames().isEmpty())
        driver.close()
    }

    @Test
    fun plainDatabaseDirectoryIsExcludedFromBackup() {
        setDirectoryExcludedFromBackup(false)

        factory.create(TestSchema, name).close()

        assertTrue(isDirectoryExcludedFromBackup())
    }

    @Test
    fun databaseDirectoryIsExcludedFromBackup() {
        setDirectoryExcludedFromBackup(false)

        factory.create(TestSchema, name, passphrase).close()

        assertTrue(isDirectoryExcludedFromBackup())
    }

    @Test
    fun persistsRowsAcrossReopen() {
        factory.create(TestSchema, name).apply {
            insertItem(id = 1, name = "Dune")
            close()
        }

        val reopened = factory.create(TestSchema, name)

        assertEquals(listOf("Dune"), reopened.itemNames())
        reopened.close()
    }

    @Test
    fun encryptedDatabaseHasNoPlainHeader() {
        factory.create(TestSchema, name, passphrase).apply {
            insertItem(id = 1, name = "Dune")
            close()
        }

        val bytes = databaseBytes()

        assertFalse(bytes.isPlainSqlite())
        assertFalse(bytes.decodeToString(throwOnInvalidSequence = false).contains("Dune"))
    }

    @Test
    fun encryptedDatabasePersistsRowsAcrossReopen() {
        factory.create(TestSchema, name, passphrase).apply {
            insertItem(id = 1, name = "Dune")
            close()
        }

        val reopened = factory.create(TestSchema, name, passphrase)

        assertEquals(listOf("Dune"), reopened.itemNames())
        reopened.close()
    }

    @Test
    fun plainDatabaseIsReplacedByEncryptedOne() {
        factory.create(TestSchema, name).apply {
            insertItem(id = 1, name = "Dune")
            close()
        }

        val encrypted = factory.create(TestSchema, name, passphrase)

        assertTrue(encrypted.itemNames().isEmpty())
        encrypted.close()
        assertFalse(databaseBytes().isPlainSqlite())
    }

    @Test
    fun databaseIsResetWhenPassphraseChanges() {
        factory.create(TestSchema, name, passphrase).apply {
            insertItem(id = 1, name = "Dune")
            close()
        }
        val newPassphrase = Random.nextBytes(32)

        factory.create(TestSchema, name, newPassphrase).apply {
            assertTrue(itemNames().isEmpty())
            insertItem(id = 2, name = "Arrival")
            close()
        }

        val reopened = factory.create(TestSchema, name, newPassphrase)
        assertEquals(listOf("Arrival"), reopened.itemNames())
        reopened.close()
    }

    @Test
    fun passphraseArrayIsNotRequiredAfterCreate() {
        val callerArray = passphrase.copyOf()
        val driver = factory.create(TestSchema, name, callerArray)
        callerArray.fill(0)

        driver.insertItem(id = 1, name = "Dune")
        driver.close()

        val reopened = factory.create(TestSchema, name, passphrase)
        assertEquals(listOf("Dune"), reopened.itemNames())
        reopened.close()
    }

    private fun databaseDirectory(): NSURL = NSURL.fileURLWithPath(
        DatabaseFileContext.databasePath(name, null).substringBeforeLast('/'),
        isDirectory = true,
    )

    @OptIn(ExperimentalForeignApi::class)
    private fun setDirectoryExcludedFromBackup(excluded: Boolean) {
        NSFileManager.defaultManager.createDirectoryAtURL(databaseDirectory(), true, null, null)
        databaseDirectory().setResourceValue(excluded, forKey = NSURLIsExcludedFromBackupKey, error = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isDirectoryExcludedFromBackup(): Boolean {
        val values = databaseDirectory().resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), error = null)
        return (values?.get(NSURLIsExcludedFromBackupKey) as? NSNumber)?.boolValue == true
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun databaseBytes(): ByteArray {
        val path = DatabaseFileContext.databasePath(name, null)
        val data = checkNotNull(NSFileManager.defaultManager.contentsAtPath(path))
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        return bytes
    }

    private fun ByteArray.isPlainSqlite(): Boolean =
        size >= SQLITE_HEADER.size && copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()
    }
}
