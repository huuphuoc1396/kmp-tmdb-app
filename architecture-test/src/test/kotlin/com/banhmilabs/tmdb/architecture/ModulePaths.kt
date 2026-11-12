package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

internal const val ROOT_PACKAGE = "com.banhmilabs.tmdb"

private val MODULE_DIR = Regex(
    "/(core/[^/]+|service/[^/]+/[^/]+|apps/[^/]+/(?:designsystem|composeApp|androidApp)" +
        "|apps/[^/]+/feature/[^/]+/[^/]+)/src/",
)

/** `…/service/title/domain/src/…` -> `service/title/domain`; `null` outside shared modules. */
internal fun KoFileDeclaration.modulePath(): String? = MODULE_DIR.find(path)?.groupValues?.get(1)

/** `apps/cmp/feature/home/ui` -> `cmp`; `null` for the shared core. */
internal fun appOf(modulePath: String): String? = modulePath.split('/').takeIf { it[0] == "apps" }?.get(1)

private val PACKAGE_SEGMENT_ALIASES = mapOf("native" to "nativeapp")

/**
 * `service/title/domain` -> `com.banhmilabs.tmdb.service.title.domain`, the same as the derived namespace
 * (`apps/native/...` -> `...apps.nativeapp...`, because `native` is a Java keyword).
 */
internal fun packageOf(modulePath: String): String = "$ROOT_PACKAGE." +
    modulePath.split('/').joinToString(".") { segment ->
        segment.replace("-", "").let { PACKAGE_SEGMENT_ALIASES[it] ?: it }
    }

/** Files whose module path matches [regex], for example `Regex("service/[^/]+/domain")`. */
internal fun KoScope.filesMatching(regex: Regex): List<KoFileDeclaration> =
    files.filter { file -> file.modulePath()?.let(regex::matches) == true }

internal fun KoFileDeclaration.importsAnyOf(prefixes: List<String>): Boolean =
    imports.any { import -> prefixes.any { import.name.startsWith(it) } }

internal fun KoFileDeclaration.importsMatching(regex: Regex): Boolean = imports.any { regex.matches(it.name) }
