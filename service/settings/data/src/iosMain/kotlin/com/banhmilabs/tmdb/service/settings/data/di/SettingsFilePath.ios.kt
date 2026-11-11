package com.banhmilabs.tmdb.service.settings.data.di

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun Scope.settingsFilePath(): Path {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("No Application Support directory")
    return directory.toPath() / SETTINGS_FILE_NAME
}

internal actual val settingsFileSystem: FileSystem = FileSystem.SYSTEM
