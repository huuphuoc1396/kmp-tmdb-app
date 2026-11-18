package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/** One-table schema for driver tests. */
internal object TestSchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        driver.execute(null, "CREATE TABLE item (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)", 0)
        return QueryResult.Unit
    }

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
}

internal fun SqlDriver.insertItem(id: Long, name: String) {
    execute(null, "INSERT INTO item (id, name) VALUES (?, ?)", 2) {
        bindLong(0, id)
        bindString(1, name)
    }
}

internal fun SqlDriver.itemNames(): List<String> = executeQuery(
    identifier = null,
    sql = "SELECT name FROM item ORDER BY id",
    mapper = { cursor ->
        QueryResult.Value(buildList { while (cursor.next().value) add(requireNotNull(cursor.getString(0))) })
    },
    parameters = 0,
).value
