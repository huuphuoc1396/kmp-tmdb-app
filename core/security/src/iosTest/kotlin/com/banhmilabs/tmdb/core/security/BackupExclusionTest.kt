package com.banhmilabs.tmdb.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class BackupExclusionTest {
    private val root = NSTemporaryDirectory() + "backup-exclusion-${Random.nextLong()}"

    @AfterTest
    fun deleteRoot() {
        NSFileManager.defaultManager.removeItemAtPath(root, error = null)
    }

    @Test
    fun marksDirectoryExcludedFromBackup() {
        NSFileManager.defaultManager.createDirectoryAtPath(root, true, null, null)

        excludeFromBackup(root)

        assertTrue(isExcludedFromBackup(root))
    }

    @Test
    fun createsMissingDirectory() {
        val directory = "$root/nested/keys"

        excludeFromBackup(directory)

        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(directory))
        assertTrue(isExcludedFromBackup(directory))
    }

    @Test
    fun isIdempotent() {
        excludeFromBackup(root)
        excludeFromBackup(root)

        assertTrue(isExcludedFromBackup(root))
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun isExcludedFromBackup(path: String): Boolean {
    val values = NSURL.fileURLWithPath(path, isDirectory = true)
        .resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), error = null)
    return (values?.get(NSURLIsExcludedFromBackupKey) as? NSNumber)?.boolValue == true
}
