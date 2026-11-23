package com.banhmilabs.tmdb.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

@OptIn(ExperimentalForeignApi::class)
fun excludeFromBackup(directory: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val excluded = NSURL.fileURLWithPath(directory, isDirectory = true)
        .setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    check(excluded) { "Could not exclude $directory from backup" }
}
