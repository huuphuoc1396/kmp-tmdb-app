package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/** iOS: databases in the app's default SQLite directory. */
fun DatabaseDriverFactory(): DatabaseDriverFactory = object : DatabaseDriverFactory {
    override fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray?): SqlDriver {
        requireNoPassphrase(passphrase)
        return NativeSqliteDriver(schema, name)
    }
}
