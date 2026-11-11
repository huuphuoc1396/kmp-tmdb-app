package com.banhmilabs.tmdb.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class PackageRulesTest {
    @Test
    fun `files use their module package`() {
        Konsist.scopeFromProject().files
            .filter { it.modulePath() != null }
            .assertTrue(additionalMessage = "Rule: files use the package of their module path") { file ->
                val expected = file.modulePath()?.let(::packageOf)
                expected != null && file.packagee?.name?.startsWith(expected) == true
            }
    }
}
