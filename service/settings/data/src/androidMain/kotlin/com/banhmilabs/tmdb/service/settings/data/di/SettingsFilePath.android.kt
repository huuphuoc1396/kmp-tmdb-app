package com.banhmilabs.tmdb.service.settings.data.di

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.settingsFilePath(): Path = androidContext().filesDir.toOkioPath() / SETTINGS_FILE_NAME

internal actual val settingsFileSystem: FileSystem = FileSystem.SYSTEM
