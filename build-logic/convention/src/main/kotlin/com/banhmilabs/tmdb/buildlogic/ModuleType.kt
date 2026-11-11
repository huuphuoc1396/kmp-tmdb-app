package com.banhmilabs.tmdb.buildlogic

/** Module kinds from `docs/architecture.md` 4.2. */
internal enum class ModuleKind {
    COMMON,
    MODEL,
    SECURITY,
    INFRA,
    DESIGNSYSTEM,
    TESTING,
    DI,
    DOMAIN,
    DATA,
    FAKE,
    NAV,
    API,
    UI,
}

/** A module's kind and, for service and feature modules, the context or feature that owns it. */
internal data class ModuleType(val kind: ModuleKind, val owner: String? = null)

private val CORE_KINDS = mapOf(
    "common" to ModuleKind.COMMON,
    "model" to ModuleKind.MODEL,
    "security" to ModuleKind.SECURITY,
    "network" to ModuleKind.INFRA,
    "database" to ModuleKind.INFRA,
    "datastore" to ModuleKind.INFRA,
    "designsystem" to ModuleKind.DESIGNSYSTEM,
    "testing" to ModuleKind.TESTING,
    "di" to ModuleKind.DI,
)

private val SERVICE_KINDS = mapOf(
    "domain" to ModuleKind.DOMAIN,
    "data" to ModuleKind.DATA,
    "fake" to ModuleKind.FAKE,
)

private val FEATURE_KINDS = mapOf(
    "nav" to ModuleKind.NAV,
    "api" to ModuleKind.API,
    "ui" to ModuleKind.UI,
)

/** `:service:title:domain` -> `ModuleType(DOMAIN, "title")`; `null` when the path matches no known type. */
internal fun moduleTypeOf(path: String): ModuleType? {
    val segments = path.removePrefix(":").split(":")
    return when {
        segments.size == 2 && segments[0] == "core" ->
            CORE_KINDS[segments[1]]?.let { ModuleType(it) }
        segments.size == 3 && segments[0] == "service" ->
            SERVICE_KINDS[segments[2]]?.let { ModuleType(it, segments[1]) }
        segments.size == 3 && segments[0] == "feature" ->
            FEATURE_KINDS[segments[2]]?.let { ModuleType(it, segments[1]) }
        else -> null
    }
}
