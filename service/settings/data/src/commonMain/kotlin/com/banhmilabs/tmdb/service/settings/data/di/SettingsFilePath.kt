package com.banhmilabs.tmdb.service.settings.data.di

import okio.FileSystem
import okio.Path
import org.koin.core.scope.Scope

/** Where the settings file lives on this platform. */
internal expect fun Scope.settingsFilePath(): Path

/** The platform file system (`FileSystem.SYSTEM` is not visible from common code). */
internal expect val settingsFileSystem: FileSystem
