package com.banhmilabs.tmdb.core.di

import org.koin.core.module.Module

/** Bindings that need the platform (Android: `androidContext()`). */
internal expect val platformModule: Module
