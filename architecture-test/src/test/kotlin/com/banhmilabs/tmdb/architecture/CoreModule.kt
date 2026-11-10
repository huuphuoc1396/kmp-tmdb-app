package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

internal const val ROOT_PACKAGE = "com.banhmilabs.tmdb"

internal fun corePackage(module: String) = "$ROOT_PACKAGE.core.$module"

internal fun KoScope.filesInCoreModule(module: String): List<KoFileDeclaration> =
    files.filter { "/core/$module/src/" in it.path }

internal fun KoFileDeclaration.importsAnyOf(prefixes: List<String>): Boolean =
    imports.any { import -> prefixes.any { import.name.startsWith(it) } }
