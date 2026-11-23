package com.banhmilabs.tmdb.service.settings.data.di

import okio.FileSystem
import okio.Path
import org.koin.core.scope.Scope

internal expect fun Scope.settingsFilePath(): Path

internal expect val settingsFileSystem: FileSystem
