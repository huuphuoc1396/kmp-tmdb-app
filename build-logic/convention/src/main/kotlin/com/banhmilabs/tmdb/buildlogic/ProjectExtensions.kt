package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

private const val ROOT_NAMESPACE = "com.banhmilabs.tmdb"

private val PACKAGE_SEGMENT_ALIASES = mapOf("native" to "nativeapp")

/** `native` is a Java keyword, which AGP rejects in a namespace, so it becomes `nativeapp`. */
internal fun packageSegment(segment: String): String =
    segment.replace("-", "").let { PACKAGE_SEGMENT_ALIASES[it] ?: it }

/** `:core:common` -> `com.banhmilabs.tmdb.core.common`; `:apps:native:designsystem` -> `…apps.nativeapp.designsystem` */
internal fun namespaceOf(path: String): String =
    "$ROOT_NAMESPACE." + path.removePrefix(":").split(":").joinToString(".", transform = ::packageSegment)

internal fun Project.derivedNamespace(): String = namespaceOf(path)
