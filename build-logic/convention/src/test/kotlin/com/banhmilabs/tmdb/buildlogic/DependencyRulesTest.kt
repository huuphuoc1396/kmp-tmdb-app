package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DependencyRulesTest {
    @Test
    fun dataToAnotherContextDataIsRejected() {
        assertRejected(":service:title:data", ":service:favorites:data")
    }

    @Test
    fun dataToOwnDomainIsAllowed() {
        assertAllowed(":service:title:data", ":service:title:domain")
        assertAllowed(":service:title:data", ":core:network")
        assertAllowed(":service:title:data", ":core:database")
        assertAllowed(":service:title:data", ":core:common")
    }

    @Test
    fun dataToOtherContextDomainIsRejected() {
        assertRejected(":service:title:data", ":service:favorites:domain")
    }

    @Test
    fun uiToUiIsRejected() {
        assertRejected(":apps:cmp:feature:home:ui", ":apps:cmp:feature:details:ui")
    }

    @Test
    fun uiToDataIsRejected() {
        assertRejected(":apps:cmp:feature:home:ui", ":service:title:data")
        assertRejected(":apps:cmp:feature:home:ui", ":core:network")
    }

    @Test
    fun fakeToDataIsRejected() {
        assertRejected(":service:title:fake", ":service:title:data")
        assertRejected(":service:title:fake", ":service:title:data", testScope = true)
    }

    @Test
    fun domainToAllowlistedDomainIsAllowed() {
        assertAllowed(":service:title:domain", ":service:favorites:domain")
    }

    @Test
    fun domainToOtherDomainIsRejected() {
        assertRejected(":service:favorites:domain", ":service:title:domain")
        assertRejected(":service:settings:domain", ":service:title:domain")
        assertRejected(":service:title:domain", ":service:title:data")
        assertRejected(":service:title:domain", ":core:network")
    }

    @Test
    fun allowlistChainIsReported() {
        assertNotNull(DependencyRules.allowlistViolation(setOf("a" to "b", "b" to "c")))
    }

    @Test
    fun shippedAllowlistIsValid() {
        assertNull(DependencyRules.allowlistViolation())
    }

    @Test
    fun fakeAndTestingAllowedOnlyInTestScope() {
        assertRejected(":apps:cmp:feature:home:ui", ":service:title:fake")
        assertAllowed(":apps:cmp:feature:home:ui", ":service:title:fake", testScope = true)
        assertRejected(":service:title:data", ":core:testing")
        assertAllowed(":service:title:data", ":core:testing", testScope = true)
    }

    @Test
    fun kernelAndInfrastructureFollowTable() {
        assertRejected(":core:common", ":core:model")
        assertRejected(":core:model", ":core:common")
        assertAllowed(":core:network", ":core:security")
        assertAllowed(":core:network", ":core:common")
        assertRejected(":core:network", ":core:database")
        assertRejected(":core:security", ":core:network")
        assertAllowed(":core:di", ":service:title:data")
    }

    @Test
    fun featureTypesFollowTable() {
        assertAllowed(":apps:cmp:feature:home:ui", ":apps:cmp:feature:details:nav")
        assertAllowed(":apps:cmp:feature:home:ui", ":apps:cmp:feature:favorites:api")
        assertAllowed(":apps:cmp:feature:home:ui", ":service:title:domain")
        assertAllowed(":apps:cmp:feature:home:ui", ":apps:cmp:designsystem")
        assertAllowed(":apps:cmp:feature:details:nav", ":core:model")
        assertRejected(":apps:cmp:feature:details:nav", ":core:common")
        assertRejected(":apps:cmp:feature:details:api", ":service:title:domain")
        assertAllowed(":apps:cmp:designsystem", ":core:model")
        assertRejected(":apps:cmp:designsystem", ":service:title:domain")
    }

    @Test
    fun uiUsesOnlyItsOwnAppFeaturesAndDesignsystem() {
        assertRejected(":apps:cmp:feature:home:ui", ":apps:native:feature:details:nav")
        assertRejected(":apps:cmp:feature:home:ui", ":apps:native:feature:cart:api")
        assertRejected(":apps:cmp:feature:home:ui", ":apps:native:designsystem")
        assertAllowed(":apps:native:feature:home:ui", ":apps:native:designsystem")
        assertAllowed(":apps:native:feature:home:ui", ":service:title:domain")
    }

    @Test
    fun appsNeverDependOnEachOther() {
        assertRejected(":apps:native:designsystem", ":apps:cmp:designsystem")
        assertRejected(":apps:native:feature:home:ui", ":apps:cmp:feature:home:ui")
    }

    @Test
    fun sharedCoreNeverDependsOnAnApp() {
        assertRejected(":core:di", ":apps:cmp:feature:home:ui")
        assertRejected(":core:model", ":apps:cmp:designsystem")
        assertRejected(":service:title:domain", ":apps:cmp:feature:details:nav")
        assertRejected(":core:di", ":apps:cmp:designsystem", testScope = true)
    }

    private fun violation(from: String, to: String, testScope: Boolean): String? =
        DependencyRules.violation(type(from), type(to), testScope)

    private fun assertAllowed(from: String, to: String, testScope: Boolean = false) {
        assertNull(violation(from, to, testScope), "$from -> $to should be allowed")
    }

    private fun assertRejected(from: String, to: String, testScope: Boolean = false) {
        assertNotNull(violation(from, to, testScope), "$from -> $to should be rejected")
    }

    private fun type(path: String) = requireNotNull(moduleTypeOf(path)) { path }
}
