package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

/** `:core:common` -> `com.banhmilabs.tmdb.core.common` */
internal fun Project.derivedNamespace(): String =
    "com.banhmilabs.tmdb." + path.removePrefix(":").split(":").joinToString(".") { it.replace("-", "") }
