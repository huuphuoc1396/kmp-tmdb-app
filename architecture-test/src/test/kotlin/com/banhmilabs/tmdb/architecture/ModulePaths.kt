package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

internal const val ROOT_PACKAGE = "com.banhmilabs.tmdb"

private val MODULE_DIR = Regex(
    "/(core/[^/]+|service/[^/]+/[^/]+|cmp/(?:designsystem|composeApp|androidApp)" +
        "|cmp/feature/[^/]+/[^/]+)/src/",
)

internal fun KoFileDeclaration.modulePath(): String? = MODULE_DIR.find(path)?.groupValues?.get(1)

internal fun packageOf(modulePath: String): String = "$ROOT_PACKAGE." + modulePath.replace('/', '.').replace("-", "")

internal fun KoScope.filesMatching(regex: Regex): List<KoFileDeclaration> =
    files.filter { file -> file.modulePath()?.let(regex::matches) == true }

internal fun KoFileDeclaration.importsAnyOf(prefixes: List<String>): Boolean =
    imports.any { import -> prefixes.any { import.name.startsWith(it) } }

internal fun KoFileDeclaration.importsMatching(regex: Regex): Boolean = imports.any { regex.matches(it.name) }
