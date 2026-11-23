package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

interface DatabaseDriverFactory {
    fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray? = null): SqlDriver
}
