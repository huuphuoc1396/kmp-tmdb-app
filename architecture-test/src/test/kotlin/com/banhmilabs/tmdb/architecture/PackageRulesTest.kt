package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class PackageRulesTest {
    @Test
    fun `files use their module package`() {
        val modules = listOf(
            "common", "model", "domain", "network", "database",
            "datastore", "security", "data", "di", "testing",
        )
        val scope = Konsist.scopeFromProject()
        modules.forEach { module ->
            scope.filesInCoreModule(module)
                .assertTrue(additionalMessage = "Rule: files in core/$module use package ${corePackage(module)}") {
                    it.packagee?.name?.startsWith(corePackage(module)) == true
                }
        }
    }
}
