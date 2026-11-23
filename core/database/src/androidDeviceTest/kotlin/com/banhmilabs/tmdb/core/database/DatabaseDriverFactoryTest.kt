package com.banhmilabs.tmdb.core.database

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseDriverFactoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val factory = DatabaseDriverFactory(context)
    private val name = "test-${Random.nextLong()}.db"
    private val passphrase = Random.nextBytes(32)

    @AfterTest
    fun deleteDatabase() {
        context.deleteDatabase(name)
    }

    @Test
    fun createsSchemaOnFirstOpen() {
        val driver = factory.create(TestSchema, name)

        assertTrue(driver.itemNames().isEmpty())
        driver.close()
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

        val bytes = context.getDatabasePath(name).readBytes()

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
        assertFalse(context.getDatabasePath(name).readBytes().isPlainSqlite())
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

    private fun ByteArray.isPlainSqlite(): Boolean =
        size >= SQLITE_HEADER.size && copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()
    }
}
