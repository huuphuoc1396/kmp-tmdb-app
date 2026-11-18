package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/** Opens SQLite databases; each context brings its own [SqlSchema]. */
interface DatabaseDriverFactory {
    /** Opens (creating or migrating) database file [name]. [passphrase] must be null until encryption exists. */
    fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray? = null): SqlDriver
}

internal fun requireNoPassphrase(passphrase: ByteArray?) =
    require(passphrase == null) { "Database encryption is not supported yet; pass a null passphrase." }
