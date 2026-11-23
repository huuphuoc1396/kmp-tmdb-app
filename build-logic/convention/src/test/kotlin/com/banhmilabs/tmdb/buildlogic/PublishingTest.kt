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
    fun fakeModulesArePublished() {
        listOf(":service:settings:fake", ":service:title:fake", ":service:favorites:fake")
            .forEach { assertTrue(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun testingAndIosModulesAreNotPublished() {
        listOf(":core:testing", ":core:ios")
            .forEach { assertFalse(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun appModulesAreNotPublished() {
        listOf(
            ":cmp:designsystem",
            ":cmp:feature:onboarding:ui",
            ":cmp:composeApp",
            ":cmp:androidApp",
        )
            .forEach { assertFalse(isPublished(requireNotNull(moduleTypeOf(it))), it) }
    }

    @Test
    fun artifactIdJoinsThePathWithDashes() {
        assertEquals("core-di", artifactIdOf(":core:di"))
        assertEquals("service-settings-domain", artifactIdOf(":service:settings:domain"))
    }
}
