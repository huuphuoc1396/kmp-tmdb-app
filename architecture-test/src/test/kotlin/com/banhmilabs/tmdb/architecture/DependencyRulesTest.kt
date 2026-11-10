package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/** Dependency rules from section 4.4 of docs/architecture.md, checked on production sources. */
class DependencyRulesTest {
    private val production = Konsist.scopeFromProduction()

    @Test
    fun `model and domain use no frameworks`() {
        val frameworks = listOf(
            "io.ktor",
            "app.cash.sqldelight",
            "androidx.",
            "org.jetbrains.compose",
            "org.koin",
            "android.",
            "platform.",
        )
        (production.filesInCoreModule("model") + production.filesInCoreModule("domain"))
            .assertFalse(additionalMessage = "Rule: model and domain use no frameworks") {
                it.importsAnyOf(frameworks)
            }
    }

    @Test
    fun `domain does not depend on implementation layers`() {
        val layers = listOf("data", "network", "database", "datastore", "security", "di").map { corePackage(it) }
        production.filesInCoreModule("domain")
            .assertFalse(additionalMessage = "Rule: domain does not depend on implementation layers") {
                it.importsAnyOf(layers)
            }
    }

    @Test
    fun `model depends on nothing in the app`() {
        production.filesInCoreModule("model")
            .assertFalse(additionalMessage = "Rule: model depends on nothing in the app") { file ->
                file.imports.any { it.name.startsWith("$ROOT_PACKAGE.") && !it.name.startsWith(corePackage("model")) }
            }
    }

    @Test
    fun `data sources do not depend on data or di`() {
        val upperLayers = listOf("data", "di").map { corePackage(it) + "." }
        listOf("network", "database", "datastore")
            .flatMap { production.filesInCoreModule(it) }
            .assertFalse(additionalMessage = "Rule: data sources do not depend on data or di") {
                it.importsAnyOf(upperLayers)
            }
    }
}
