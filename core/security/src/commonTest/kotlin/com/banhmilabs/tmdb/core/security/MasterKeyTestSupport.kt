package com.banhmilabs.tmdb.core.security

/** Removes the key stored for [alias] so platform key stores don't keep test keys. */
internal expect fun deleteMasterKeyForTest(alias: String)
