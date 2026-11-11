package com.banhmilabs.tmdb.service.settings.data.di

import okio.FileSystem
import okio.Path
import org.koin.core.scope.Scope

/** The JVM target is for tests only. */
internal actual fun Scope.settingsFilePath(): Path =
    FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "kmp-tmdb" / SETTINGS_FILE_NAME

internal actual val settingsFileSystem: FileSystem = FileSystem.SYSTEM
