package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/** Opens SQLite databases; each context brings its own [SqlSchema]. */
interface DatabaseDriverFactory {
    /**
     * Opens (creating or migrating) database file [name]. [passphrase] encrypts the database; a database that cannot
     * be opened with it is deleted and created again. Platforms that don't support encryption reject a non-null value.
     */
    fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray? = null): SqlDriver
}
