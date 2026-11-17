package com.banhmilabs.tmdb.buildlogic

/** Module kinds from `docs/architecture.md` 5.2. */
internal enum class ModuleKind {
    COMMON,
    MODEL,
    SECURITY,
    INFRA,
    DESIGNSYSTEM,
    TESTING,
    DI,
    IOS_SDK,
    DOMAIN,
    DATA,
    FAKE,
    NAV,
    API,
    UI,
    APP,
    ANDROID_APP,
}

/**
 * A module's kind; for service and feature modules, the context or feature that owns it; for modules under `cmp/`,
 * the app they belong to (`null` for the shared core).
 */
internal data class ModuleType(val kind: ModuleKind, val owner: String? = null, val app: String? = null)

private val CORE_KINDS = mapOf(
    "common" to ModuleKind.COMMON,
    "model" to ModuleKind.MODEL,
    "security" to ModuleKind.SECURITY,
    "network" to ModuleKind.INFRA,
    "database" to ModuleKind.INFRA,
    "datastore" to ModuleKind.INFRA,
    "testing" to ModuleKind.TESTING,
    "di" to ModuleKind.DI,
    "ios" to ModuleKind.IOS_SDK,
)

private val SERVICE_KINDS = mapOf(
    "domain" to ModuleKind.DOMAIN,
    "data" to ModuleKind.DATA,
    "fake" to ModuleKind.FAKE,
)

private const val CMP_APP = "cmp"

private val APP_KINDS = mapOf(
    "designsystem" to ModuleKind.DESIGNSYSTEM,
    "composeApp" to ModuleKind.APP,
    "androidApp" to ModuleKind.ANDROID_APP,
)

private val FEATURE_KINDS = mapOf(
    "nav" to ModuleKind.NAV,
    "api" to ModuleKind.API,
    "ui" to ModuleKind.UI,
)

/**
 * `:service:title:domain` -> `ModuleType(DOMAIN, "title")`; `:cmp:feature:home:ui` -> `ModuleType(UI, "home",
 * "cmp")`; `null` when the path matches no known type.
 */
internal fun moduleTypeOf(path: String): ModuleType? {
    val segments = path.removePrefix(":").split(":")
    return when {
        segments.size == 2 && segments[0] == "core" ->
            CORE_KINDS[segments[1]]?.let { ModuleType(it) }
        segments.size == 3 && segments[0] == "service" ->
            SERVICE_KINDS[segments[2]]?.let { ModuleType(it, segments[1]) }
        segments.size == 2 && segments[0] == CMP_APP ->
            APP_KINDS[segments[1]]?.let { ModuleType(it, app = CMP_APP) }
        segments.size == 4 && segments[0] == CMP_APP && segments[1] == "feature" ->
            FEATURE_KINDS[segments[3]]?.let { ModuleType(it, segments[2], CMP_APP) }
        else -> null
    }
}
