package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryDriverTest {
    @Test
    fun inMemoryDriverCreatesSchemaAndStoresRows() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TestSchema.create(driver)

        driver.insertItem(id = 1, name = "Dune")

        assertEquals(listOf("Dune"), driver.itemNames())
        driver.close()
    }
}
