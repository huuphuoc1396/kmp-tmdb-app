package com.banhmilabs.tmdb.core.database

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Runs on an Android device: a real SQLite file. */
class DatabaseDriverFactoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val factory = DatabaseDriverFactory(context)
    private val name = "test-${Random.nextLong()}.db"

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
    fun rejectsPassphrase() {
        assertFailsWith<IllegalArgumentException> { factory.create(TestSchema, name, byteArrayOf(1)) }
        assertFalse(context.getDatabasePath(name).exists())
    }
}
