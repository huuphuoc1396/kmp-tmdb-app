package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/** Import rules from section 4.3 of docs/architecture.md, checked on production sources. */
class DependencyRulesTest {
    private val production = Konsist.scopeFromProduction()
    private val forbiddenInUi =
        Regex(Regex.escape(ROOT_PACKAGE) + """\.(feature\.[^.]+\.ui|service\.[^.]+\.data)(\..*)?""")

    @Test
    fun `model, domain and nav use no frameworks`() {
        val frameworks = listOf(
            "io.ktor",
            "app.cash.sqldelight",
            "androidx.",
            "org.jetbrains.compose",
            "org.koin",
            "android.",
            "platform.",
        )
        production.filesMatching(Regex("core/model|service/[^/]+/domain|feature/[^/]+/nav"))
            .assertFalse(additionalMessage = "Rule: model, domain and nav use no frameworks") {
                it.importsAnyOf(frameworks)
            }
    }

    @Test
    fun `domain imports no data, fake or infrastructure`() {
        val forbidden = Regex(
            Regex.escape(ROOT_PACKAGE) +
                """\.(service\.[^.]+\.(data|fake)|core\.(network|database|datastore|security|di))(\..*)?""",
        )
        production.filesMatching(Regex("service/[^/]+/domain"))
            .assertFalse(additionalMessage = "Rule: domain imports no data, fake or infrastructure") {
                it.importsMatching(forbidden)
            }
    }

    @Test
    fun `model depends on nothing in the app`() {
        production.filesMatching(Regex("core/model"))
            .assertFalse(additionalMessage = "Rule: model depends on nothing in the app") { file ->
                val model = packageOf("core/model")
                file.imports.any { it.name.startsWith("$ROOT_PACKAGE.") && !it.name.startsWith(model) }
            }
    }

    @Test
    fun `infrastructure does not depend on services or di`() {
        val upperLayers = listOf("$ROOT_PACKAGE.service.", packageOf("core/di") + ".")
        production.filesMatching(Regex("core/(network|database|datastore)"))
            .assertFalse(additionalMessage = "Rule: infrastructure does not depend on services or di") {
                it.importsAnyOf(upperLayers)
            }
    }

    @Test
    fun `feature ui imports no other feature ui and no service data`() {
        production.filesMatching(Regex("feature/[^/]+/ui"))
            .assertFalse(
                additionalMessage = "Rule: feature ui imports no other feature ui and no service data",
            ) { file ->
                val own = file.modulePath()?.let(::packageOf).orEmpty()
                file.imports.any { forbiddenInUi.matches(it.name) && !it.name.startsWith(own) }
            }
    }
}
