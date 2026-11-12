---
paths:
  - "build-logic/**"
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle.properties"
  - "gradle/libs.versions.toml"
---

# Build logic

## Responsibility
- `build-logic/convention` (an included build, see `settings.gradle.kts` `pluginManagement { includeBuild("build-logic") }`) owns every shared build setting: targets, SDK levels, toolchain, test dependencies, Detekt, Kover.
- Module `build.gradle.kts` files only apply plugins and declare project/library dependencies. No `android {}`, `jvmToolchain`, targets or test setup there.
- Repositories are declared only in `settings.gradle.kts` (`RepositoriesMode.FAIL_ON_PROJECT_REPOS`); a `repositories {}` block in a module fails the build.

## Pattern
- Convention plugin: one class per file in the default package, `build-logic/convention/src/main/kotlin/<Name>ConventionPlugin.kt`, with a one-line KDoc:
  ```kotlin
  class XConventionPlugin : Plugin<Project> {
      override fun apply(target: Project) = with(target) {
          pluginManager.apply("<plugin id>")
          extensions.configure<XExtension> { ... }
      }
  }
  ```
- Read the catalog through `Project.libs`, `libs.findLibrary("<alias>").get()` and `libs.version("<alias>")` from `com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt`. Never hardcode a version or SDK level.
- Shared configuration lives in `internal fun Project.configureX()` functions in `com/banhmilabs/tmdb/buildlogic/` (`configureKotlinMultiplatform()`, `configureKover()`); plugins call them.
- Magic numbers go in a `private const val` (`MIN_LINE_COVERAGE` in `buildlogic/Kover.kt`).
- Gradle plugin artifacts are `compileOnly` in `build-logic/convention/build.gradle.kts`.
- References: `ModuleConventionPlugin.kt` (type from path, dependency check), `buildlogic/KotlinMultiplatform.kt` (targets + test deps), `buildlogic/Kover.kt` (extension config + constant), `core/network/build.gradle.kts` (module file shape).

## Module build file
- `plugins { alias(libs.plugins.tmdb.module) }` and nothing else: the type (from the path) decides KMP targets, Detekt, and Kover for `domain` / `data`.
- Dependencies go in `kotlin { sourceSets { commonMain.dependencies { ... } } }` with type-safe accessors (`projects.core.security`).
- Use `api(...)` only when the module exposes the dependency's types (`service/*/domain` → `core:model`, `core:common`, and an allowlisted `domain`); otherwise `implementation(...)`.
- Allowed project dependencies per type are in `docs/architecture.md` 5.3 and enforced by `tmdb.module`: a forbidden one fails configuration with `Rule: <from> -> <to>: …`. `service/*/fake` and `core:testing` only in test source sets (source-set name contains `Test`). Only the configurations behind Kotlin source-set dependencies are checked; plugin- and IDE-created configurations (for example the Kotlin plugin's SwiftPM lock-file classpath) are not.
- `tmdb.module` already applies `tmdb.detekt` and the kotlinx.serialization compiler plugin; don't apply them again in KMP modules.

## Module types and dependency rules
- `buildlogic/ModuleType.kt`: `moduleTypeOf(path)` maps `:core:<name>`, `:service:<ctx>:domain|data|fake`, `:apps:<app>:designsystem`, `:apps:<app>:composeApp` (`APP`), `:apps:<app>:androidApp` (`ANDROID_APP`), `:apps:<app>:feature:<x>:nav|api|ui` to a `ModuleType(kind, owner, app)`; any other path returns `null` and `tmdb.module` fails with "Unknown module type".
- Per type: `ANDROID_APP` gets `configureAndroidApplication()` (`buildlogic/AndroidApplication.kt`, not KMP); every other type gets `configureKotlinMultiplatform()` (`APP` also builds the static iOS framework `ComposeApp`); `apps/cmp` modules of kind `DESIGNSYSTEM`, `UI`, `APP` also get `configureComposeMultiplatform()` (`buildlogic/Compose.kt`). App shells (`APP`, `ANDROID_APP`) are roots: they may depend on their own app and the shared core.
- `buildlogic/DependencyRules.kt`: `violation(from, to, testScope)` is the 5.3 table; `domainAllowlist` holds the allowed `domain → domain` pairs (`title` → `favorites`), and `allowlistViolation()` rejects chains deeper than one level. `appViolation()` rejects shared core → app and app → other app before the table is consulted.
- `tmdb.module` sets `group` to the parent package of the path so klib names stay unique when leaf names repeat.
- Changing the architecture: update `docs/architecture.md` 5.2 / 5.3 first (with the user's agreement), then `moduleTypeOf` / `DependencyRules` and their tests in `build-logic/convention/src/test/` (JUnit Jupiter; `kotlin-test` doesn't match the `kotlin-dsl` compiler).

## Adding a convention plugin
1. If it wraps a third-party Gradle plugin: add the version, a `<name>-gradlePlugin` library and the plugin id to `gradle/libs.versions.toml`; add `compileOnly(libs.<name>.gradlePlugin)` to `build-logic/convention/build.gradle.kts`; declare the plugin with `apply false` in the root `build.gradle.kts`.
2. Write `<Name>ConventionPlugin.kt`.
3. Register it in `gradlePlugin { plugins { register("<name>") { id = libs.plugins.tmdb.<name>.get().pluginId; implementationClass = "<Name>ConventionPlugin" } } }`.
4. Add `tmdb-<name> = { id = "tmdb.<name>" }` (no version) under `[plugins]`.
5. Apply it from `tmdb.module` for the types that need it, or in non-shared modules with `alias(libs.plugins.tmdb.<name>)`.

## Adding a library
1. `[versions]` entry (latest stable on Maven Central; check it compiles with the project's Kotlin and JDK 21).
2. `[libraries]` entry with `version.ref`.
3. Use `libs.<alias>` in a module, or `libs.findLibrary("<alias>")` in a convention plugin when every module needs it.

## Do / Don't
- Keep build logic configuration-cache safe (`org.gradle.configuration-cache=true`): no `project` access inside task actions; declare extra inputs explicitly, like `inputs.files(...)` in `architecture-test/build.gradle.kts`.
- Unit test deps are set in `configureKotlinMultiplatform()`: `commonTest` has `kotlin-test` (other multiplatform test libraries such as Turbine and Kotest assertions go there once a module needs them); `androidDeviceTest` has the AndroidX test runner; MockK and other JVM-only libraries only on `jvmTest`.
- Root `build.gradle.kts` applies Kover with no rules (so `koverVerify` exists with no gated module) and makes the root `jvmTest` depend on `build-logic`'s `:convention:test`.

## Testing
- `./gradlew jvmTest` after any build-logic change (runs the `build-logic` unit tests too); `./gradlew compileTestKotlinJvm` after module build changes; `./gradlew assemble` (macOS + Xcode) when targets, Android config or plugins change.
- When adding or removing tasks, confirm with `./gradlew :core:common:tasks --all | grep <task>`.
