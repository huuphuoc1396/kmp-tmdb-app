# Plan: Main branch skeleton

Requirement: [Main branch skeleton](task.md)
Status: Done

## Summary
Create the Gradle build from scratch: a wrapper, one version catalog, and an included `build-logic` build with three convention plugins (KMP library, Detekt, Kover). On top of it, add the ten `core/*` modules with the dependency graph from section 4.4 of the architecture document and one smoke test each. Add a JVM-only `:architecture-test` module that enforces the dependency rules with Konsist, a GitHub Actions workflow that runs every check on Ubuntu, and an update to the CI section of the architecture document. Modules contain no production code; the next tasks add logic to them.

## Decisions
- **Toolchain versions**: Gradle 9.9.0, Kotlin 2.4.21, AGP 9.5.1, Kover 0.9.11, Konsist 0.18.1, JUnit 6.1.3 (BOM), JDK 21 toolchain. These are the latest stable releases on Maven Central / the Gradle Plugin Portal on 2026-11-10.
- **Detekt 2.0.0-alpha.6** with the ktlint wrapper rule set, chosen by the user over 1.23.8. It is built for Kotlin 2.4 / Gradle 9, while 1.23.8 targets Kotlin 2.0. This is the one exception to the "stable versions" constraint.
- **Android KMP library plugin** (`com.android.kotlin.multiplatform.library`): AGP 9 no longer supports `com.android.library` together with the Kotlin Multiplatform plugin.
- **Namespace derived from the project path**: `:core:common` becomes `com.banhmilabs.tmdb.core.common`, so a module's build file only needs the plugin and its dependencies (AC-3). The derivation removes `-` from path segments.
- **Smoke test per module**, chosen by the user over adding small real types. No production code is added.
- **Module dependencies declared now**, following section 4.4, chosen by the user over adding them later.
- **Konsist in its own JVM module `:architecture-test`**, chosen by the user over `core:testing`.
- **Kover applied only to `core:domain` and `core:data`**, each verifying ≥ 80% line coverage on JVM tests, with DI, DTOs and generated code excluded.
- **iOS 16 minimum**: stored as `ios-deploymentTarget` in the version catalog. `main` produces no iOS framework, so the value has no effect yet. Umbrella frameworks on `cmp`/`native` will read it.
- **One local command runs every CI check**: `./gradlew detekt jvmTest koverVerify :architecture-test:test`. It is documented in the architecture document, and CI runs the same command.
- **iOS targets skipped on non-macOS hosts**: set `kotlin.native.ignoreDisabledTargets=true` so Linux CI prints a warning and does not fail (EC-1).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Module list, package layout, dependency rules | Architecture document | `docs/architecture.md` §4.1, §4.4 |
| Ignore rules for build output and secrets | `.gitignore` | `.gitignore` (already covers `build/`, `.gradle/`, `.kotlin/`, `local.properties`, `secrets.properties`) |

The repository has no code yet, so nothing else can be reused.

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (new) | Gradle wrapper 9.9.0 |
| root | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` (new) | Root build, module list, single version catalog (AC-3) |
| `build-logic/convention` | `settings.gradle.kts`, `convention/build.gradle.kts`, `KmpLibraryConventionPlugin.kt`, `DetektConventionPlugin.kt`, `KoverConventionPlugin.kt`, `ProjectExtensions.kt` (new) | Shared build configuration (AC-1, AC-3, AC-4, AC-5) |
| root | `config/detekt/detekt.yml` (new) | Detekt and ktlint rules (AC-4) |
| `core/*` ×10 | `build.gradle.kts`, `src/commonTest/kotlin/.../<Name>SmokeTest.kt` (new) | Empty modules with tests (AC-1, AC-2) |
| `architecture-test` | `build.gradle.kts`, `src/test/kotlin/.../DependencyRulesTest.kt`, `PackageRulesTest.kt` (new) | Konsist rules (AC-6) |
| root | `.github/workflows/ci.yml` (new) | CI on Ubuntu (AC-7, EC-4) |
| docs | `docs/architecture.md` (modify) | CI section matches JVM-only CI; local check command (AC-8) |

## Steps

### Step 1: Gradle wrapper, root build and version catalog  (AC-3) ✅
- Files: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` (new); `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` (new)
- What:
  - Generate the wrapper with the cached Gradle distribution:
    `~/.gradle/wrapper/dists/gradle-9.8.1-bin/*/gradle-9.8.1/bin/gradle wrapper --gradle-version 9.9.0 --distribution-type bin`.
  - `gradle/libs.versions.toml`:
    - `[versions]`:
      - toolchain: `kotlin = "2.4.21"`, `agp = "9.5.1"`, `jvmToolchain = "21"`
      - platforms: `android-compileSdk = "36"`, `android-minSdk = "24"`, `ios-deploymentTarget = "16.0"`
      - tools and tests: `detekt = "2.0.0-alpha.6"`, `kover = "0.9.11"`, `konsist = "0.18.1"`, `junit = "6.1.3"`
    - `[libraries]`:
      - `kotlin-test`, `junit-bom`, `junit-jupiter`, `junit-platform-launcher`, `konsist`
      - `detekt-rules-ktlint-wrapper` (`dev.detekt:detekt-rules-ktlint-wrapper`)
      - Gradle plugin artifacts for build-logic: `kotlin-gradlePlugin`, `android-gradlePlugin`, `detekt-gradlePlugin`, `kover-gradlePlugin`
    - `[plugins]`:
      - `kotlin-multiplatform`, `kotlin-jvm`, `android-kotlin-multiplatform-library`, `detekt`, `kover`
      - convention plugin ids: `tmdb-kmp-library`, `tmdb-detekt`, `tmdb-kover` (no version)
  - `settings.gradle.kts`:
    - `pluginManagement { includeBuild("build-logic"); repositories { google(); mavenCentral(); gradlePluginPortal() } }`
    - `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS`, repositories `google()` and `mavenCentral()`
    - `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`, `rootProject.name = "kmp-tmdb-app"`
    - No `include` yet.
  - `build.gradle.kts`: declare `kotlin-multiplatform`, `kotlin-jvm`, `android-kotlin-multiplatform-library`, `detekt`, `kover` with `apply false`.
  - `gradle.properties`:
    - `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`
    - `org.gradle.caching=true`, `org.gradle.configuration-cache=true`
    - `kotlin.code.style=official`, `kotlin.native.ignoreDisabledTargets=true`
- Unit tests: none.
- Compile: `./gradlew help`

### Step 2: build-logic with the KMP library convention  (AC-1, AC-3) ✅
- Files: `build-logic/settings.gradle.kts`, `build-logic/convention/build.gradle.kts`, `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt` (new); `settings.gradle.kts` (modify)
- What:
  - `build-logic/settings.gradle.kts`: reuse the root catalog through `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`, add the same repositories, and `include(":convention")`.
  - `convention/build.gradle.kts`: `kotlin-dsl`, `jvmToolchain(21)`, and `compileOnly` on the four plugin artifacts. Register `tmdb.kmp.library` with `KmpLibraryConventionPlugin` in `gradlePlugin { plugins { ... } }`.
  - `ProjectExtensions.kt`:
    - `val Project.libs: VersionCatalog`
    - `fun Project.derivedNamespace(): String`
  - `KmpLibraryConventionPlugin`:
    - Applies `org.jetbrains.kotlin.multiplatform` and `com.android.kotlin.multiplatform.library`.
    - Configures `kotlin { jvmToolchain(21); jvm(); iosArm64(); iosSimulatorArm64() }`.
    - Configures `androidLibrary { namespace = derivedNamespace(); compileSdk = 36; minSdk = 24 }`, reading values from the catalog.
    - Adds `commonTest` dependency `kotlin("test")`.
    - Does not enable `explicitApi()`: the modules are internal to the app.
  - Root `settings.gradle.kts`: unchanged in this step; Step 3 adds the modules.
- Pseudo-code:
  ```kotlin
  fun Project.derivedNamespace() =
      "com.banhmilabs.tmdb." + path.removePrefix(":").split(":").joinToString(".") { it.replace("-", "") }
  ```
- Unit tests: none.
- Compile: `./gradlew help`

### Step 3: The ten core modules with dependencies and smoke tests  (AC-1, AC-2, AC-3) ✅
- Files: `core/<name>/build.gradle.kts` and `core/<name>/src/commonTest/kotlin/com/banhmilabs/tmdb/core/<name>/<Name>SmokeTest.kt` for `common`, `model`, `domain`, `network`, `database`, `datastore`, `security`, `data`, `di`, `testing` (new); `settings.gradle.kts` (modify: `include(":core:common", ... )`)
- What:
  - Each `build.gradle.kts` contains `plugins { alias(libs.plugins.tmdb.kmp.library) }` plus the `commonMain` dependencies from section 4.4:

    | Module | `commonMain` dependencies |
    |---|---|
    | `common` | none |
    | `model` | none |
    | `security` | none |
    | `domain` | `api(projects.core.model)`, `api(projects.core.common)` |
    | `network` | `implementation(projects.core.security)` |
    | `database` | `implementation(projects.core.security)` |
    | `datastore` | `implementation(projects.core.security)` |
    | `data` | `implementation(projects.core.domain)`, `implementation(projects.core.network)`, `implementation(projects.core.database)`, `implementation(projects.core.datastore)` |
    | `di` | `implementation(projects.core.data)` |
    | `testing` | `api(projects.core.domain)` |

  - Each smoke test, for example:
    ```kotlin
    package com.banhmilabs.tmdb.core.common

    class CommonSmokeTest {
        @Test
        fun testSourceSetRuns() {
            assertTrue(true, "Placeholder: replace when the module gets production code")
        }
    }
    ```
- Unit tests: `<Name>SmokeTest.testSourceSetRuns` in each of the ten modules (runs on JVM, Android and iOS simulator targets).
- Compile: `./gradlew assemble`

### Step 4: Detekt with ktlint rules  (AC-4) ✅
- Files: `build-logic/convention/src/main/kotlin/DetektConventionPlugin.kt` (new), `config/detekt/detekt.yml` (new), `build-logic/convention/build.gradle.kts` (modify: register `tmdb.detekt`), `KmpLibraryConventionPlugin.kt` (modify: apply `tmdb.detekt`)
- What:
  - `DetektConventionPlugin`:
    - Applies `dev.detekt` and sets `detekt { buildUponDefaultConfig = true; config.setFrom(rootProject.file("config/detekt/detekt.yml")); parallel = true }`.
    - Sets `source` to every `src/*/kotlin` directory of the project.
    - Adds `detektPlugins(libs.detekt.rules.ktlint.wrapper)`.
    - Enables plain-text and SARIF reports.
  - `detekt.yml`: start from the default config. Overrides:
    - Enable the `ktlint` rule set.
    - Set `MaxLineLength` to 120 to match the ktlint default.
    - Allow `@Test` function names in camelCase without an extra rule.
- Unit tests: none (Detekt checks itself).
- Compile: `./gradlew detekt`

### Step 5: Kover on domain and data  (AC-5, EC-3) ✅
- Files: `build-logic/convention/src/main/kotlin/KoverConventionPlugin.kt` (new), `build-logic/convention/build.gradle.kts` (modify: register `tmdb.kover`), `core/domain/build.gradle.kts`, `core/data/build.gradle.kts` (modify: add `alias(libs.plugins.tmdb.kover)`)
- What:
  - `KoverConventionPlugin` applies `org.jetbrains.kotlinx.kover` and configures:
    ```kotlin
    kover {
        reports {
            filters { excludes {
                packages("*.di", "*.di.*")
                classes("*Dto", "*Dto\$*", "*.generated.*", "*BuildKonfig*")
            } }
            verify { rule { minBound(80, CoverageUnit.LINE) } }
        }
    }
    ```
  - Kover measures JVM tests, which matches the JVM-only CI.
- Unit tests: none.
- Compile: `./gradlew koverVerify`

### Step 6: Konsist architecture tests  (AC-6) ✅
- Files: `architecture-test/build.gradle.kts`, `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/DependencyRulesTest.kt`, `PackageRulesTest.kt` (new); `settings.gradle.kts` (modify: `include(":architecture-test")`)
- What:
  - `build.gradle.kts`:
    - Plugins: `kotlin-jvm` and `tmdb.detekt`, with `jvmToolchain(21)`.
    - Test dependencies: `konsist`, `kotlin-test`, `platform(junit-bom)`, `junit-jupiter`; `testRuntimeOnly(junit-platform-launcher)`.
    - `tasks.test { useJUnitPlatform() }`.
    - Inputs: declare `core/` as an input directory of `test`, so a source change re-runs the task with build cache and configuration cache on.
  - `DependencyRulesTest`: production files only (`Konsist.scopeFromProduction()` filtered by module path). Each rule fails with a message that names the rule:
    1. `model and domain use no frameworks`: files in `core/model` and `core/domain` import nothing starting with `io.ktor`, `app.cash.sqldelight`, `androidx.`, `org.jetbrains.compose`, `org.koin`, `android.` or `platform.`.
    2. `domain does not depend on implementation layers`: files in `core/domain` import nothing from `com.banhmilabs.tmdb.core.{data,network,database,datastore,security,di}`.
    3. `model depends on nothing in the app`: files in `core/model` import nothing from `com.banhmilabs.tmdb.` outside `core.model`.
    4. `data sources do not depend on data or di`: files in `core/network`, `core/database` and `core/datastore` import nothing from `com.banhmilabs.tmdb.core.{data,di}`.
  - `PackageRulesTest`: every file under `core/<name>` (production and test) has a package that starts with `com.banhmilabs.tmdb.core.<name>`.
  - Empty production scope: each assertion uses Konsist's non-strict mode, so a module with no files passes.
- Pseudo-code:
  ```kotlin
  @Test
  fun `domain does not depend on implementation layers`() {
      Konsist.scopeFromProduction().files
          .withPath("core/domain/")   // or filter { it.path.contains("/core/domain/") }
          .assertFalse(strict = false, additionalMessage = "Rule: domain does not depend on implementation layers") { file ->
              file.imports.any { imp -> forbiddenLayers.any { imp.name.startsWith(it) } }
          }
  }
  ```
- Unit tests: `DependencyRulesTest` (4 cases above), `PackageRulesTest.filesUseTheirModulePackage`.
- Compile: `./gradlew :architecture-test:compileTestKotlin`

### Step 7: CI workflow  (AC-7, EC-1, EC-4) ✅
- Files: `.github/workflows/ci.yml` (new)
- What:
  - Triggers: `push` to `main`, `pull_request` targeting `main`.
  - Settings: `permissions: contents: read`; `concurrency` group per ref with `cancel-in-progress: true`.
  - One job `check` on `ubuntu-latest` with `timeout-minutes: 30`:
    - `actions/checkout@v5`
    - `actions/setup-java@v5` (temurin 21)
    - `gradle/actions/setup-gradle@v5` (cache read-only on pull requests)
    - `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue`
    - On failure, `actions/upload-artifact@v4` uploads `**/build/reports/**`
  - No secrets used, so fork pull requests run the same checks (EC-4). iOS targets are skipped on Linux with a warning (EC-1).
- Unit tests: none.
- Compile: none (YAML). Checked manually by the first CI run after push.

### Step 8: Update the architecture document  (AC-8) ✅
- Files: `docs/architecture.md` (modify)
- What:
  - Section 10 (CI), `main` row: `detekt`, `jvmTest`, `koverVerify`, `:architecture-test:test` on Ubuntu (JVM only). Add a note that Android and iOS targets are compiled locally, not on `main` CI.
  - Add a short "Running checks locally" paragraph with `./gradlew detekt jvmTest koverVerify :architecture-test:test` and `./gradlew assemble` (all targets, needs macOS + Xcode).
  - Section 4.1: mention the `architecture-test` module next to `build-logic`.
- Unit tests: none.
- Compile: none.

After the last step, run: `./gradlew assemble detekt jvmTest koverVerify :architecture-test:test`.

## Traceability
| AC / EC | Step(s) | Test(s) / check |
|---|---|---|
| AC-1 | 2, 3 | `./gradlew assemble` on macOS compiles Android, iosArm64, iosSimulatorArm64 and JVM |
| AC-2 | 3 | `./gradlew jvmTest`: 10 `<Name>SmokeTest` classes pass |
| AC-3 | 1, 2, 3 | Review: versions only in `libs.versions.toml`; each core `build.gradle.kts` has only the plugin alias and dependencies |
| AC-4 | 4 | Manual: add an unused import or a wrong indent to a smoke test → `./gradlew detekt` fails, naming file and rule; revert |
| AC-5 | 5 | `./gradlew koverVerify` passes on the skeleton. Manual: add an untested class to `core:domain` → it fails; revert |
| AC-6 | 6 | `DependencyRulesTest`, `PackageRulesTest`. Manual: add `import io.ktor.client.HttpClient` to a file in `core:domain` → test fails with the rule name; revert |
| AC-7 | 7 | First push to GitHub: the `check` job runs and is green |
| AC-8 | 8 | Review of `docs/architecture.md` §10 |
| EC-1 | 1, 7 | CI on Ubuntu passes with a disabled-target warning for iOS |
| EC-2 | — | Nothing reads `secrets.properties`; build passes without it (local run) |
| EC-3 | 5, 6 | `koverVerify` and Konsist tests pass on modules without production code |
| EC-4 | 7 | Workflow uses no secrets; review of `ci.yml` |

## Out of scope
- API key hiding and `secrets.properties`: network task.
- Real code in any module, and the libraries it needs (Ktor, SQLDelight, DataStore, Koin, coroutines): added by the tasks that use them.
- Android host tests and iOS tests on CI: CI stays JVM-only by decision in task.md.
- App modules and iOS frameworks: `cmp` and `native` branches.

## Risks
- **Detekt 2.0 is alpha.** Plugin id, task names or KMP source handling may differ from the docs. Mitigation: if `detekt` does not pick up KMP source sets, set `source` explicitly as described in Step 4. If the alpha cannot run with Kotlin 2.4.21 at all, stop and ask the user (fallback: 1.23.8 + ktlint-gradle).
- **Kover on a module with no classes** may report an error instead of passing (EC-3). Mitigation: check in Step 5. If it fails, stop and ask the user before changing the rule.
- **Konsist on an empty production scope**: assertions must not fail on empty lists. Mitigation: non-strict assertions; check in Step 6.
- **Konsist and KMP paths**: source sets are `src/commonMain/kotlin`, so module filtering is done on the file path (`/core/<name>/src/`), not by Gradle module name.
- **Configuration cache with Konsist**: Konsist reads files that Gradle does not track, so a stale result could be reused. Mitigation: `core/` is declared as a test input (Step 6).
- **AGP 9.5.1 with Gradle 9.9.0**: newest pair and not yet tested together here. Mitigation: if `./gradlew help` fails in Step 1, fall back to the newest Gradle version that AGP 9.5.1 documents as supported.

## Deviations
- Step 2: the Android target also enables `withHostTest {}`. Without it, AGP warns on every build that `commonTest` exists but Android host tests are off. The smoke tests now also run on the Android host.
- Step 2: `ProjectExtensions.kt` also has `VersionCatalog.version(alias)` to read catalog versions.
- Step 4: Detekt 2 has no plain-text report, so HTML and SARIF reports are enabled. The console still prints file, line and rule. `source` is set to the whole `src/` directory, which covers every source set.
- Step 6: shared helpers (`filesInCoreModule`, `importsAnyOf`, `corePackage`) live in `CoreModule.kt` in the test sources. Assertions use Konsist's default non-strict mode, so empty scopes pass. Test inputs are declared as `core/**/*.kt` (excluding `build/`), not the whole `core/` directory, so build output does not invalidate the test task.
