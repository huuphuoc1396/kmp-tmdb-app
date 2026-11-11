package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModuleTypeTest {
    @Test
    fun coreModulesMapToTheirKinds() {
        assertEquals(ModuleType(ModuleKind.COMMON), moduleTypeOf(":core:common"))
        assertEquals(ModuleType(ModuleKind.MODEL), moduleTypeOf(":core:model"))
        assertEquals(ModuleType(ModuleKind.SECURITY), moduleTypeOf(":core:security"))
        assertEquals(ModuleType(ModuleKind.INFRA), moduleTypeOf(":core:network"))
        assertEquals(ModuleType(ModuleKind.INFRA), moduleTypeOf(":core:database"))
        assertEquals(ModuleType(ModuleKind.INFRA), moduleTypeOf(":core:datastore"))
        assertEquals(ModuleType(ModuleKind.TESTING), moduleTypeOf(":core:testing"))
        assertEquals(ModuleType(ModuleKind.DI), moduleTypeOf(":core:di"))
    }

    @Test
    fun serviceModulesCarryTheirContext() {
        assertEquals(ModuleType(ModuleKind.DOMAIN, "title"), moduleTypeOf(":service:title:domain"))
        assertEquals(ModuleType(ModuleKind.DATA, "favorites"), moduleTypeOf(":service:favorites:data"))
        assertEquals(ModuleType(ModuleKind.FAKE, "settings"), moduleTypeOf(":service:settings:fake"))
    }

    @Test
    fun featureModulesCarryTheirFeature() {
        assertEquals(ModuleType(ModuleKind.NAV, "details"), moduleTypeOf(":feature:details:nav"))
        assertEquals(ModuleType(ModuleKind.API, "favorites"), moduleTypeOf(":feature:favorites:api"))
        assertEquals(ModuleType(ModuleKind.UI, "home"), moduleTypeOf(":feature:home:ui"))
    }

    @Test
    fun designsystemIsKnown() {
        assertEquals(ModuleType(ModuleKind.DESIGNSYSTEM), moduleTypeOf(":core:designsystem"))
    }

    @Test
    fun unknownPathsReturnNull() {
        listOf(
            ":core:foo",
            ":core:foo:bar",
            ":core:domain",
            ":core:data",
            ":service:title",
            ":service:title:impl",
            ":feature:home",
            ":feature:home:impl",
            ":tools:x",
            ":architecture-test",
        ).forEach { assertNull(moduleTypeOf(it), it) }
    }
}
