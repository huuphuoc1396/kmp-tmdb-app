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
- Magic numbers go in a `private companion object` constant (`MIN_LINE_COVERAGE` in `KoverConventionPlugin.kt`).
- Gradle plugin artifacts are `compileOnly` in `build-logic/convention/build.gradle.kts`.
- References: `KmpLibraryConventionPlugin.kt` (targets + test deps), `KoverConventionPlugin.kt` (extension config + constant), `core/data/build.gradle.kts` (module file shape).

## Module build file
- `plugins { alias(libs.plugins.tmdb.kmp.library) }`, plus `alias(libs.plugins.tmdb.kover)` only for coverage-gated modules (`core:domain`, `core:data`).
- Dependencies go in `kotlin { sourceSets { commonMain.dependencies { ... } } }` with type-safe accessors (`projects.core.domain`).
- Use `api(...)` only when the module exposes the dependency's types (`core:domain` → `model`, `common`; `core:testing` → `domain`); otherwise `implementation(...)`.
- `tmdb.kmp.library` already applies `tmdb.detekt`; don't apply it again in KMP modules.

## Adding a convention plugin
1. If it wraps a third-party Gradle plugin: add the version, a `<name>-gradlePlugin` library and the plugin id to `gradle/libs.versions.toml`; add `compileOnly(libs.<name>.gradlePlugin)` to `build-logic/convention/build.gradle.kts`; declare the plugin with `apply false` in the root `build.gradle.kts`.
2. Write `<Name>ConventionPlugin.kt`.
3. Register it in `gradlePlugin { plugins { register("<name>") { id = libs.plugins.tmdb.<name>.get().pluginId; implementationClass = "<Name>ConventionPlugin" } } }`.
4. Add `tmdb-<name> = { id = "tmdb.<name>" }` (no version) under `[plugins]`.
5. Apply it in modules with `alias(libs.plugins.tmdb.<name>)`.

## Adding a library
1. `[versions]` entry (latest stable on Maven Central; check it compiles with the project's Kotlin and JDK 21).
2. `[libraries]` entry with `version.ref`.
3. Use `libs.<alias>` in a module, or `libs.findLibrary("<alias>")` in a convention plugin when every module needs it.

## Do / Don't
- Keep build logic configuration-cache safe (`org.gradle.configuration-cache=true`): no `project` access inside task actions; declare extra inputs explicitly, like `inputs.files(...)` in `architecture-test/build.gradle.kts`.
- Don't add a `Test` source set dependency to `commonTest`; unit test deps belong on `jvmTest` in `KmpLibraryConventionPlugin`.

## Testing
- `./gradlew compileTestKotlinJvm` after any build change; `./gradlew assemble` (macOS + Xcode) when targets, Android config or plugins change.
- When adding or removing tasks, confirm with `./gradlew :core:common:tasks --all | grep <task>`.
