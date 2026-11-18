package com.banhmilabs.tmdb.core.database

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/** Android: databases in the app's database directory. */
fun DatabaseDriverFactory(context: Context): DatabaseDriverFactory = object : DatabaseDriverFactory {
    override fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray?): SqlDriver {
        requireNoPassphrase(passphrase)
        return AndroidSqliteDriver(schema, context.applicationContext, name)
    }
}
