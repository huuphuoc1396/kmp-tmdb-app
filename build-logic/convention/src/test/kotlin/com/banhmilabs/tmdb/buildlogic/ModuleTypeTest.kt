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
    fun featureModulesCarryTheirFeatureAndApp() {
        assertEquals(ModuleType(ModuleKind.NAV, "details", "cmp"), moduleTypeOf(":apps:cmp:feature:details:nav"))
        assertEquals(ModuleType(ModuleKind.API, "cart", "cmp"), moduleTypeOf(":apps:cmp:feature:cart:api"))
        assertEquals(ModuleType(ModuleKind.UI, "home", "cmp"), moduleTypeOf(":apps:cmp:feature:home:ui"))
    }

    @Test
    fun designsystemBelongsToAnApp() {
        assertEquals(ModuleType(ModuleKind.DESIGNSYSTEM, app = "cmp"), moduleTypeOf(":apps:cmp:designsystem"))
    }

    @Test
    fun composeAppPathIsApp() {
        assertEquals(ModuleType(ModuleKind.APP, app = "cmp"), moduleTypeOf(":apps:cmp:composeApp"))
    }

    @Test
    fun androidAppPathIsAndroidApp() {
        assertEquals(ModuleType(ModuleKind.ANDROID_APP, app = "cmp"), moduleTypeOf(":apps:cmp:androidApp"))
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
            ":core:designsystem",
            ":feature:home:ui",
            ":feature:details:nav",
            ":apps:cmp",
            ":apps:cmp:feature:home",
            ":apps:cmp:feature:home:impl",
            ":apps:cmp:android:feature:home:ui",
            ":apps:cmp:desktopApp",
            ":tools:x",
            ":architecture-test",
        ).forEach { assertNull(moduleTypeOf(it), it) }
    }
}
