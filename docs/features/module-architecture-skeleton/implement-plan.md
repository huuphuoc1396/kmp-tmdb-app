# Plan: Module architecture skeleton

Requirement: [task.md](task.md)
Status: Done

## Summary
Replace the two convention plugins `tmdb.kmp.library` and `tmdb.kover` with one `tmdb.module` plugin that reads the module type from the Gradle path, configures the module for that type, and fails configuration on any `project(...)` dependency that the table in `docs/architecture.md` 4.3 forbids. The type table and the dependency rules are plain Kotlin in `build-logic`, covered by JUnit tests that `./gradlew jvmTest` also runs. Then remove `core:domain` / `core:data`, extend the Konsist rules to `service/` and `feature/`, and bring the docs in line.

## Decisions
- `core/ui` (listed in `docs/architecture.md` 4.7) is merged into `core/designsystem`: no separate type. — User's choice; the alternative was a separate `core/ui` type.
- Rule table and path parsing are unit-tested in `build-logic`, and the root `jvmTest` task depends on those tests, so the CI command stays unchanged. — User's choice over a separate CI step (changes the command) or manual checks only.
- Unique klib names come from `project.group` set to the parent package of the path (`:service:title:domain` → group `com.banhmilabs.tmdb.service.title`, name `domain`). — User's choice over setting `moduleName` per compilation.
- Build-logic tests use JUnit Jupiter assertions (`junit-bom` + `junit-jupiter`, already in the catalog), not `kotlin-test`. — `kotlin-dsl` compiles with Gradle's embedded Kotlin; `kotlin-test` 2.4.21 would pull a newer stdlib than that compiler reads.
- `koverVerify` keeps working with no coverage-gated module by applying the Kover plugin (no rules) to the root project. — Root `koverVerify` then always exists; each `domain` / `data` module still verifies its own 80% rule. Today's empty `core:domain` / `core:data` already pass verification with no classes, so an empty root passes too.
- The `domain → domain` allowlist lives in `build-logic` (`DependencyRules.domainAllowlist`) and starts with `title → favorites`. "One level only" is checked on the allowlist itself: a context may not appear both as a source and as a target. — Any real dependency must be allowlisted, so a valid allowlist makes deeper chains impossible.
- Test source sets are recognised by configuration name: any configuration whose name contains `Test` (`jvmTestImplementation`, `iosTestImplementation`, …) is test scope. — KMP names test configurations this way; no other configuration contains `Test`.
- `core:di` and `core:testing` get no new dependencies now; they gain them with their first code. — No speculative dependencies.
- `architecture-test` and `build-logic` stay outside `tmdb.module` (EC-1). App or umbrella modules on `cmp` / `native` (`composeApp`, `androidApp`, `shared`) will use their own plugins and are not checked by the type rules.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| KMP targets, Android namespace, `jvmTest` deps | `KmpLibraryConventionPlugin` body | `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` |
| Kover filters and 80% rule | `KoverConventionPlugin` body | `build-logic/convention/src/main/kotlin/KoverConventionPlugin.kt` |
| Catalog access, namespace from path | `libs`, `version()`, `derivedNamespace()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt` |
| Detekt setup | `tmdb.detekt` | `build-logic/convention/src/main/kotlin/DetektConventionPlugin.kt` |
| JUnit Jupiter test setup (JVM module) | `architecture-test/build.gradle.kts` | same |
| Konsist helpers | `ROOT_PACKAGE`, `importsAnyOf` | `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/CoreModule.kt` |

## Changes overview
| Module | Files (new / modified / deleted) | Why |
|---|---|---|
| `build-logic/convention` | `buildlogic/ModuleType.kt`, `buildlogic/DependencyRules.kt`, `buildlogic/KotlinMultiplatform.kt`, `buildlogic/Kover.kt`, `ModuleConventionPlugin.kt` (new); tests `ModuleTypeTest.kt`, `DependencyRulesTest.kt` (new); `build.gradle.kts` (modify); `KmpLibraryConventionPlugin.kt`, `KoverConventionPlugin.kt` (delete) | Type inference, dependency check, one plugin |
| root | `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` (modify) | Apply Kover at root, hook build-logic tests into `jvmTest`, drop removed modules, swap plugin aliases |
| `core:*` (8 modules) | `build.gradle.kts` (modify) | Apply `tmdb.module`; `di` and `testing` drop removed deps |
| `core:domain`, `core:data` | whole directories (delete) | Removed by the task |
| `architecture-test` | `ModulePaths.kt` (new, replaces `CoreModule.kt`), `PackageRulesTest.kt`, `DependencyRulesTest.kt`, `build.gradle.kts` (modify) | Rules over `core/`, `service/`, `feature/` |
| docs | `CLAUDE.md`, `docs/architecture.md`, `.claude/rules/build-logic.md`, `.claude/rules/testing.md`, `.claude/rules/architecture-test.md` (modify) | Describe the new state (AC-9) |

## Steps

### Step 1: Module types and dependency rules as plain Kotlin  (AC-3, AC-4, AC-5, EC-2, EC-3) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ModuleType.kt` (new), `.../buildlogic/DependencyRules.kt` (new), `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/ModuleTypeTest.kt` (new), `.../DependencyRulesTest.kt` (new), `build-logic/convention/build.gradle.kts` (modify)
- What:
  - `internal enum class ModuleKind { COMMON, MODEL, SECURITY, INFRA, DESIGNSYSTEM, TESTING, DI, DOMAIN, DATA, FAKE, NAV, API, UI }`
  - `internal data class ModuleType(val kind: ModuleKind, val owner: String? = null)`: `owner` is the context (service) or feature name.
  - `internal fun moduleTypeOf(path: String): ModuleType?`, `null` for an unknown path:
    - `:core:common` → COMMON, `:core:model` → MODEL, `:core:security` → SECURITY
    - `:core:network|database|datastore` → INFRA, `:core:designsystem` → DESIGNSYSTEM
    - `:core:testing` → TESTING, `:core:di` → DI
    - `:service:<ctx>:domain|data|fake` → DOMAIN / DATA / FAKE with `owner = ctx`
    - `:feature:<x>:nav|api|ui` → NAV / API / UI with `owner = x`
    - anything else (including `:core:foo`, `:core:foo:bar`, `:service:x`) → `null`
  - `internal object DependencyRules`:
    - `val domainAllowlist: Set<Pair<String, String>> = setOf("title" to "favorites")`
    - `fun violation(from: ModuleType, to: ModuleType, testScope: Boolean): String?`: `null` when allowed, otherwise the rule text, for example `"data depends only on its own context's domain, kernel and infrastructure"`.
    - `fun allowlistViolation(): String?`: non-null when a context is both a source and a target in the allowlist.
  - Allowed table (production scope):

    | from | to |
    |---|---|
    | COMMON, MODEL | — |
    | SECURITY | COMMON, MODEL |
    | INFRA | COMMON, MODEL, SECURITY |
    | DESIGNSYSTEM | MODEL |
    | TESTING | COMMON, MODEL |
    | DI | any |
    | DOMAIN | COMMON, MODEL; DOMAIN when `(from.owner to to.owner)` is in the allowlist |
    | DATA | DOMAIN with the same owner; COMMON, MODEL, SECURITY, INFRA |
    | FAKE | DOMAIN with the same owner; COMMON, MODEL |
    | NAV, API | MODEL |
    | UI | NAV, API, DOMAIN (any owner); DESIGNSYSTEM, COMMON, MODEL |

    Test scope additionally allows FAKE (any owner) and TESTING.
  - `build.gradle.kts`: `testImplementation(platform(libs.junit.bom))`, `testImplementation(libs.junit.jupiter)`, `testRuntimeOnly(libs.junit.platform.launcher)`, `tasks.test { useJUnitPlatform() }`.
- Unit tests:
  - `ModuleTypeTest`:
    - `coreModulesMapToTheirKinds`
    - `serviceModulesCarryTheirContext`
    - `featureModulesCarryTheirFeature`
    - `designsystemIsKnown` (EC-3)
    - `unknownPathsReturnNull` (AC-5): `:core:foo`, `:core:foo:bar`, `:service:title`, `:tools:x`
  - `DependencyRulesTest`:
    - `dataToAnotherContextDataIsRejected` (AC-3)
    - `dataToOwnDomainIsAllowed`
    - `dataToOtherContextDomainIsRejected`
    - `uiToUiIsRejected`
    - `uiToDataIsRejected`
    - `fakeToDataIsRejected`
    - `domainToAllowlistedDomainIsAllowed`
    - `domainToOtherDomainIsRejected` (AC-4)
    - `allowlistChainIsReported` (AC-4: a copy with `a→b, b→c` gives a violation)
    - `shippedAllowlistIsValid`
    - `fakeAndTestingAllowedOnlyInTestScope` (EC-2)
    - `featureTypesFollowTable` (EC-3)
- Compile: `./gradlew -p build-logic :convention:test`

### Step 2: The `tmdb.module` plugin  (AC-2, AC-3, AC-4, AC-5) ✅
- Files: `.../buildlogic/KotlinMultiplatform.kt` (new), `.../buildlogic/Kover.kt` (new), `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` (new), `build-logic/convention/build.gradle.kts` (modify), `gradle/libs.versions.toml` (modify)
- What:
  - Move the body of `KmpLibraryConventionPlugin.apply` into `internal fun Project.configureKotlinMultiplatform()`, and the body of `KoverConventionPlugin.apply` (with `MIN_LINE_COVERAGE`) into `internal fun Project.configureKover()`. The old plugin classes call these functions until step 3 deletes them, so nothing breaks in between.
  - `class ModuleConventionPlugin : Plugin<Project>`, with KDoc "Shared KMP module configured by the type its path implies; fails on dependencies the architecture forbids."
    ```kotlin
    override fun apply(target: Project) = with(target) {
        val type = moduleTypeOf(path) ?: throw GradleException(
            "Unknown module type for $path. Shared modules live at :core:<name>, " +
                ":service:<context>:domain|data|fake or :feature:<name>:nav|api|ui (docs/architecture.md 4.2).",
        )
        DependencyRules.allowlistViolation()?.let { throw GradleException(it) }
        group = derivedNamespace().substringBeforeLast('.')
        configureKotlinMultiplatform()
        if (type.kind == ModuleKind.DOMAIN || type.kind == ModuleKind.DATA) configureKover()
        enforceDependencyRules(path, type)
    }
    ```
  - `private fun Project.enforceDependencyRules(fromPath: String, from: ModuleType)`:
    ```kotlin
    configurations.configureEach {
        val testScope = "Test" in name
        dependencies.withType<ProjectDependency>().configureEach {
            val toPath = path   // ProjectDependency.path, Gradle ≥ 8.11
            val to = moduleTypeOf(toPath)
                ?: throw GradleException("Rule: $fromPath -> $toPath: target is not a shared module")
            DependencyRules.violation(from, to, testScope)?.let {
                throw GradleException("Rule: $fromPath -> $toPath: $it (docs/architecture.md 4.3)")
            }
        }
    }
    ```
    Only strings and the immutable `ModuleType` are captured, so the configuration cache is not affected (EC-4).
  - Register `module` in `gradlePlugin { plugins { … } }` (`id = libs.plugins.tmdb.module.get().pluginId`, `implementationClass = "ModuleConventionPlugin"`).
  - Catalog: add `tmdb-module = { id = "tmdb.module" }`.
- Unit tests: covered by step 1 (the plugin only wires the rules).
- Compile: `./gradlew -p build-logic :convention:test`, then `./gradlew help` (old plugins still in use)

### Step 3: Switch modules, remove `core:domain` / `core:data`  (AC-1, AC-2, AC-7) ✅
- Files:
  - `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` (modify)
  - `core/{common,model,network,database,datastore,security,di,testing}/build.gradle.kts` (modify)
  - `core/domain/`, `core/data/` (delete)
  - `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`, `KoverConventionPlugin.kt` (delete)
  - `build-logic/convention/build.gradle.kts` (modify)
- What:
  - `settings.gradle.kts`: drop `":core:domain"` and `":core:data"` from `include(...)`.
  - Every `core/*/build.gradle.kts`: `plugins { alias(libs.plugins.tmdb.module) }`.
    - `network`, `database`, `datastore` keep `implementation(projects.core.security)`.
    - `di` and `testing` lose their `dependencies` blocks (`projects.core.data` and `projects.core.domain` are gone).
  - Delete the two old plugin classes and their `register("kmpLibrary")` / `register("kover")` entries.
  - Catalog: remove `tmdb-kmp-library` and `tmdb-kover`.
  - Root `build.gradle.kts`:
    - change `alias(libs.plugins.kover) apply false` to `alias(libs.plugins.kover)`, so the root project gets a rule-less `koverVerify`;
    - add:
      ```kotlin
      // Run build-logic unit tests as part of `./gradlew jvmTest`.
      tasks.register("jvmTest") {
          dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
      }
      ```
- Unit tests: none new (step 1 tests now run through `jvmTest`).
- Compile: `./gradlew compileTestKotlinJvm`, then `./gradlew jvmTest koverVerify`

### Step 4: Architecture tests over `core/`, `service/`, `feature/`  (AC-6) ✅
- Files: `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/ModulePaths.kt` (new, replaces `CoreModule.kt`), `PackageRulesTest.kt`, `DependencyRulesTest.kt`, `architecture-test/build.gradle.kts` (modify)
- What:
  - `ModulePaths.kt`:
    - keep `ROOT_PACKAGE` and `importsAnyOf`;
    - add `internal fun KoFileDeclaration.modulePath(): String?`, which returns the first match of `/(core/[^/]+|service/[^/]+/[^/]+|feature/[^/]+/[^/]+)/src/` in `path`;
    - add `internal fun packageOf(modulePath: String)`: `"$ROOT_PACKAGE." + modulePath.replace('/', '.').replace("-", "")`, matching `derivedNamespace()`;
    - add `internal fun KoScope.filesMatching(regex: Regex)`, which filters by `modulePath()`.
  - `PackageRulesTest`: rewrite `files use their module package` over every file with a non-null `modulePath()`, asserting that the package starts with `packageOf(modulePath)`. The hardcoded module list goes away.
  - `DependencyRulesTest` (production scope):
    - `model, domain and nav use no frameworks`: `core/model`, `service/*/domain`, `feature/*/nav`; same framework prefixes as today.
    - `domain imports no data, fake or infrastructure`: no import matching `…service\.[^.]+\.(data|fake)`, nor `core.network|database|datastore|security|di`.
    - `model depends on nothing in the app`: kept.
    - `infrastructure does not depend on services or di`: `core/network|database|datastore` import nothing under `…service.` or `…core.di`. This replaces `data sources do not depend on data or di`.
    - `feature ui imports no other feature ui and no service data`.
  - `build.gradle.kts`: add `rootProject.fileTree("service")` and `rootProject.fileTree("feature")` (same include/exclude) to `inputs.files`.
- Unit tests: these are the tests. Konsist assertions are non-strict, so empty file lists pass, as today's empty production sets do.
- Compile: `./gradlew :architecture-test:test`

### Step 5: Docs match the new state  (AC-9) ✅
- Files: `CLAUDE.md`, `docs/architecture.md`, `.claude/rules/build-logic.md`, `.claude/rules/testing.md`, `.claude/rules/architecture-test.md` (modify)
- What:
  - `CLAUDE.md`:
    - Current state: no `core:domain` / `core:data`; service contexts are created with the first feature that needs them.
    - Commands: coverage covers `domain` / `data` modules, plus a rule-less root `koverVerify`.
    - Modules: all shared modules apply `tmdb.module`; drop the "planned" notes.
    - Enforcement: the `tmdb.module` dependency check plus Konsist over `core/`, `service/`, `feature/`.
    - Conventions: a new module only needs its path and `alias(libs.plugins.tmdb.module)`; it is no longer added to `PackageRulesTest`.
    - Reuse table: `tmdb.module`, `tmdb.detekt`.
  - `docs/architecture.md`:
    - Remove the "Status" note.
    - 4.1: say the service tree is created on demand.
    - 4.2 / 4.3: `core/designsystem` also holds image loading, `UiText` and Compose resources.
    - 4.6: describe the enforcement as in place. The allowlist lives in `build-logic` (`DependencyRules`); an unknown path is rejected.
    - 4.7: remove `core/ui`.
    - 11: mark the migration step done.
  - `.claude/rules/build-logic.md`:
    - module build file = `alias(libs.plugins.tmdb.module)` plus dependencies;
    - remove the "Target build logic (planned)" section and describe `ModuleType` / `DependencyRules` as they exist;
    - adding a module type means extending `moduleTypeOf`, the table and their tests;
    - references point at `ModuleConventionPlugin.kt`.
  - `.claude/rules/testing.md`: `core:testing` depends only on the kernel; Kover runs on `domain` / `data`; drop the "until the migration" notes.
  - `.claude/rules/architecture-test.md`: `ModulePaths.kt` helpers; no module list to maintain; inputs cover `core/`, `service/`, `feature/`; drop the "Target rules" section and describe the rules as they now are.
- Unit tests: none (docs).
- Compile: none.

After the last step run: `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue`, then `./gradlew assemble` (macOS + Xcode), then the manual checks in Traceability.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 3 | Manual: `settings.gradle.kts` lists no `core:domain`, `core:data` or `service:*`; directories gone |
| AC-2 | 2, 3 | Manual: every `core/*/build.gradle.kts` applies only `tmdb.module`; `./gradlew :core:common:tasks --all \| grep koverVerify` is empty |
| AC-3 | 1, 2 | `DependencyRulesTest.dataToAnotherContextDataIsRejected`. Manual: temporary `:service:a:data` depending on `:service:b:data` → `./gradlew help` fails with `Rule: :service:a:data -> :service:b:data: …`; revert |
| AC-4 | 1, 2 | `domainToOtherDomainIsRejected`, `allowlistChainIsReported`. Manual: temporary `:service:a:domain` → `:service:b:domain` fails; revert |
| AC-5 | 1, 2 | `ModuleTypeTest.unknownPathsReturnNull`. Manual: temporary `:core:foo:bar` applying `tmdb.module` → `./gradlew help` fails with "Unknown module type"; revert |
| AC-6 | 4 | Manual: temporary file in `service/x/domain/src/commonMain/...` importing `io.ktor.client.HttpClient`, then with a wrong package → `./gradlew :architecture-test:test` fails with the rule; revert |
| AC-7 | 3 | `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` passes; `jvmTest` output shows `:build-logic:convention:test` ran |
| AC-8 | 2, 3 | `./gradlew assemble` on macOS. Manual: temporary `:service:a:domain` and `:service:b:domain` with one class each → `assemble` builds with no duplicate klib name; revert |
| AC-9 | 5 | Manual review of the five docs |
| EC-1 | 3 | `./gradlew :architecture-test:test` still builds with `tmdb.detekt` only |
| EC-2 | 1 | `DependencyRulesTest.fakeAndTestingAllowedOnlyInTestScope` |
| EC-3 | 1 | `ModuleTypeTest.designsystemIsKnown`, `DependencyRulesTest.featureTypesFollowTable` |
| EC-4 | 2 | Manual: run `./gradlew help` twice → second run prints "Reusing configuration cache" |

## Out of scope
- Any `service/*`, `fake` or feature module (created by features).
- Compose setup for `ui` / `designsystem` (done on `cmp`).
- Updating the onboarding subtasks and plans (follow-up right after this task).
- `kotlin.time.Clock` (decided with the first code that needs time).

## Risks
- `gradle.includedBuild("build-logic")` may not expose tasks of a build included through `pluginManagement { includeBuild(...) }`. — If step 3 fails there, move `includeBuild("build-logic")` to the top level of `settings.gradle.kts` (top-level included builds also contribute plugins), then retry.
- Kover on a root project with no Kotlin sources might not create `koverVerify`, or might fail on empty data. — If so, register a root `koverVerify` that only depends on the subproject tasks (`dependsOn(subprojects.mapNotNull { it.tasks.findByName("koverVerify") })` is not configuration-cache friendly, so use `tasks.register("koverVerify")` with no actions instead), and record the change in this plan.
- Setting `group` may not be enough for unique Kotlin/Native klib names. — The AC-8 manual check catches it; the fallback is setting `compilerOptions.moduleName` per compilation, recorded as a deviation.
- Throwing inside `dependencies.withType<ProjectDependency>().configureEach` might surface as a generic build-script error. — The message keeps the `Rule:` prefix and both paths, so it stays readable; checked in the AC-3 manual step.

## Deviations
- `tmdb.module` checks only the configurations behind Kotlin source-set dependencies (`api`, `implementation`, `compileOnly`, `runtimeOnly` of each source set), and test scope comes from the source-set name. Checking every configuration failed in two ways: the Kotlin and Android plugins add dependencies of a module on itself (found in the AC-3 manual check), and Android Studio sync failed on the Kotlin plugin's `swiftPMDependenciesForLockFilesMetadataClasspathDependencies`, which collects the dependencies of every project (`Rule: :core:common -> :core:database`). Error messages name the configuration.
- `DependencyRules.violation` reports a dedicated message ("fake and core:testing are allowed only in test source sets") when a production source set depends on `fake` or `core:testing`; `core:di` is exempt as a composition root.
- `DependencyRules` takes the allowlist as a parameter (default `domainAllowlist`) so tests can check chains without changing the shipped list.
- Risks 1 and 2 did not happen: `gradle.includedBuild("build-logic")` exposes `:convention:test` from the `pluginManagement` include, and the root project with Kover applied has a passing `koverVerify`.
