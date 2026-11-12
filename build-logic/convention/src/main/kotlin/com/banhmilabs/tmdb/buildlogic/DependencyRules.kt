package com.banhmilabs.tmdb.buildlogic

import com.banhmilabs.tmdb.buildlogic.ModuleKind.ANDROID_APP
import com.banhmilabs.tmdb.buildlogic.ModuleKind.API
import com.banhmilabs.tmdb.buildlogic.ModuleKind.APP
import com.banhmilabs.tmdb.buildlogic.ModuleKind.COMMON
import com.banhmilabs.tmdb.buildlogic.ModuleKind.DATA
import com.banhmilabs.tmdb.buildlogic.ModuleKind.DESIGNSYSTEM
import com.banhmilabs.tmdb.buildlogic.ModuleKind.DI
import com.banhmilabs.tmdb.buildlogic.ModuleKind.DOMAIN
import com.banhmilabs.tmdb.buildlogic.ModuleKind.FAKE
import com.banhmilabs.tmdb.buildlogic.ModuleKind.INFRA
import com.banhmilabs.tmdb.buildlogic.ModuleKind.MODEL
import com.banhmilabs.tmdb.buildlogic.ModuleKind.NAV
import com.banhmilabs.tmdb.buildlogic.ModuleKind.SECURITY
import com.banhmilabs.tmdb.buildlogic.ModuleKind.TESTING
import com.banhmilabs.tmdb.buildlogic.ModuleKind.UI

/** Allowed module dependencies from `docs/architecture.md` 5.3 and 9. */
internal object DependencyRules {
    /** Allowed `domain -> domain` pairs, as `source context to target context`, one level deep. */
    val domainAllowlist: Set<Pair<String, String>> = setOf("title" to "favorites")

    private val KERNEL = setOf(COMMON, MODEL)
    private val TEST_ONLY = setOf(FAKE, TESTING)

    /** Returns `null` when [from] may depend on [to], otherwise the broken rule. */
    fun violation(
        from: ModuleType,
        to: ModuleType,
        testScope: Boolean,
        allowlist: Set<Pair<String, String>> = domainAllowlist,
    ): String? {
        appViolation(from, to)?.let { return it }
        if (to.kind in TEST_ONLY && from.kind != DI) {
            return if (testScope) null else "fake and core:testing are allowed only in test source sets"
        }
        val allowed = when (from.kind) {
            COMMON, MODEL -> false
            SECURITY, TESTING -> to.kind in KERNEL
            INFRA -> to.kind in KERNEL || to.kind == SECURITY
            DESIGNSYSTEM, NAV, API -> to.kind == MODEL
            DI, APP, ANDROID_APP -> true
            DOMAIN -> to.kind in KERNEL || (to.kind == DOMAIN && (from.owner to to.owner) in allowlist)
            DATA -> to.kind in KERNEL || to.kind in setOf(SECURITY, INFRA) || isOwnDomain(from, to)
            FAKE -> to.kind in KERNEL || isOwnDomain(from, to)
            UI -> to.kind in KERNEL || to.kind in setOf(NAV, API, DOMAIN, DESIGNSYSTEM)
        }
        return if (allowed) null else ruleFor(from.kind)
    }

    /** Returns a message when a context is both a source and a target, which would allow chains deeper than one. */
    fun allowlistViolation(allowlist: Set<Pair<String, String>> = domainAllowlist): String? {
        val chained = allowlist.map { it.first }.toSet() intersect allowlist.map { it.second }.toSet()
        return if (chained.isEmpty()) {
            null
        } else {
            "domain -> domain allowlist must be one level deep; $chained appear as both source and target"
        }
    }

    /** The shared core never depends on an app, and one app never depends on another. */
    private fun appViolation(from: ModuleType, to: ModuleType): String? = when {
        to.app == null || to.app == from.app -> null
        from.app == null -> "the shared core (core, service) never depends on an app"
        else -> "apps never depend on each other"
    }

    private fun isOwnDomain(from: ModuleType, to: ModuleType) = to.kind == DOMAIN && to.owner == from.owner

    private fun ruleFor(kind: ModuleKind): String = when (kind) {
        COMMON, MODEL -> "kernel modules depend on nothing"
        SECURITY, TESTING -> "${kind.label} depends only on the kernel"
        INFRA -> "infrastructure depends only on the kernel and core:security"
        DESIGNSYSTEM, NAV, API -> "${kind.label} depends only on core:model"
        DI -> "core:di may depend on anything"
        APP, ANDROID_APP -> "an app shell may depend on its own app and the shared core"
        DOMAIN -> "domain depends only on the kernel and allowlisted domains of other contexts"
        DATA -> "data depends only on its own context's domain, the kernel and infrastructure"
        FAKE -> "fake depends only on its own context's domain and the kernel"
        UI -> "ui depends only on nav/api and the designsystem of its app, service domains and the kernel"
    }

    private val ModuleKind.label get() = name.lowercase()
}
