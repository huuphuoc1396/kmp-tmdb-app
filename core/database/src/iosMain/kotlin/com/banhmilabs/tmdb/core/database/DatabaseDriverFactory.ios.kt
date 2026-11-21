package com.banhmilabs.tmdb.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.interop.SQLiteExceptionErrorCode
import co.touchlab.sqliter.interop.SqliteErrorType
import com.banhmilabs.tmdb.core.security.excludeFromBackup

/**
 * iOS: databases in the app's default SQLite directory, which is excluded from iCloud and Finder backups;
 * encrypted with SQLCipher when given a passphrase.
 */
fun DatabaseDriverFactory(): DatabaseDriverFactory = object : DatabaseDriverFactory {
    override fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, passphrase: ByteArray?): SqlDriver {
        val driver = when (passphrase) {
            null -> NativeSqliteDriver(schema, name)
            else -> openEncrypted(schema, name, passphrase.toHex())
        }
        excludeFromBackup(DatabaseFileContext.databasePath(name, null).substringBeforeLast('/'))
        return driver
    }
}

/** Opens [name] now, so a wrong key fails here; a file that isn't a database for [hexKey] is recreated. */
private fun openEncrypted(schema: SqlSchema<QueryResult.Value<Unit>>, name: String, hexKey: String): SqlDriver {
    fun open(): SqlDriver = NativeSqliteDriver(schema, name, onConfiguration = {
        it.copy(encryptionConfig = DatabaseConfiguration.Encryption(key = hexKey))
    }).also { driver ->
        driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master",
            mapper = { cursor -> QueryResult.Value(cursor.next().value) },
            parameters = 0,
        )
    }

    return try {
        open()
    } catch (e: SQLiteExceptionErrorCode) {
        if (e.errorType != SqliteErrorType.SQLITE_NOTADB) throw e
        DatabaseFileContext.deleteDatabase(name)
        open()
    }
}

private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS_PER_BYTE = 2

/** Lowercase hex, two characters per byte: the passphrase text SQLCipher derives its key from. */
private fun ByteArray.toHex(): String =
    joinToString("") { (it.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS_PER_BYTE, '0') }
