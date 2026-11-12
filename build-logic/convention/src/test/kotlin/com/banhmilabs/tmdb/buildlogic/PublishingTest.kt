package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublishingTest {
    @Test
    fun productionCoreAndServiceModulesArePublished() {
        listOf(
            ":core:common",
            ":core:model",
            ":core:security",
            ":core:network",
            ":core:database",
            ":core:datastore",
            ":core:di",
            ":service:settings:domain",
            ":service:settings:data",
        ).forEach { assertTrue(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun testOnlyModulesAreNotPublished() {
        listOf(":core:testing", ":service:settings:fake")
            .forEach { assertFalse(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun appModulesAreNotPublished() {
        listOf(
            ":apps:cmp:designsystem",
            ":apps:cmp:feature:onboarding:ui",
            ":apps:cmp:composeApp",
            ":apps:cmp:androidApp",
        )
            .forEach { assertFalse(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun artifactIdJoinsThePathWithDashes() {
        assertEquals("core-di", artifactIdOf(":core:di"))
        assertEquals("service-settings-domain", artifactIdOf(":service:settings:domain"))
    }
}
