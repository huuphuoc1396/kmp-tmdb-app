package com.banhmilabs.tmdb.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SQLiteNotADatabaseException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

fun DatabaseDriverFactory(context: Context): DatabaseDriverFactory = object : DatabaseDriverFactory {
    override fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray?): SqlDriver =
        if (passphrase == null) {
            AndroidSqliteDriver(schema, context.applicationContext, name)
        } else {
            openEncrypted(context.applicationContext, schema, name, passphrase)
        }
}

private val sqlCipherLoaded: Unit by lazy { System.loadLibrary("sqlcipher") }

private fun openEncrypted(
    context: Context,
    schema: SqlSchema<QueryResult.Value<Unit>>,
    name: String,
    passphrase: ByteArray,
): SqlDriver {
    sqlCipherLoaded
    fun open(): SupportSQLiteOpenHelper = SupportOpenHelperFactory(passphrase.copyOf())
        .create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(AndroidSqliteDriver.Callback(schema))
                .build(),
        )
        .also { it.writableDatabase }

    val helper = try {
        open()
    } catch (_: SQLiteNotADatabaseException) {
        context.deleteDatabase(name)
        open()
    }
    return AndroidSqliteDriver(helper)
}
