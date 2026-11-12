# Plan: Publish shared core

Requirement: [01 - Publish shared core](../subtasks/01-publish-shared-core.md)
Status: Done

## Summary
`tmdb.module` applies `maven-publish` to every production module of the shared core (kinds `COMMON`, `MODEL`,
`SECURITY`, `INFRA`, `DI`, `DOMAIN`, `DATA`). It publishes all of that module's Kotlin Multiplatform
publications, which are the root Gradle-metadata module plus Android (AAR), JVM and iOS, to `build/maven-repo` at
the repo root:
- Coordinates: one group `com.banhmilabs.tmdb` and an artifact named after the path (`core-di`,
  `service-settings-domain`).
- Version: `0.1.0-SNAPSHOT` from the version catalog.

Test-only kinds (`FAKE`, `TESTING`) and app modules are never published. A `publishSharedCore` task in each
published module gives one command for everything, `./gradlew publishSharedCore`. Inter-module dependencies in the
POM and Gradle metadata point at these coordinates, so a consumer declares only what it uses and gets the rest
transitively.

## Decisions
- **Publish every target** (from user): the root module, Android, JVM, iosArm64 and iosSimulatorArm64. Publishing
  the iOS klibs needs macOS + Xcode. Elsewhere, `kotlin.native.ignoreDisabledTargets=true` skips the iOS targets,
  as for `assemble`. Rejected: Android + root metadata only.
- **Coordinates `com.banhmilabs.tmdb:<path-with-dashes>`** (from user), for example `com.banhmilabs.tmdb:core-di`,
  `com.banhmilabs.tmdb:service-settings-domain`, and the per-target `…-android` / `…-jvm` / `…-iosarm64`. Only
  the publications get these coordinates: `project.group` stays the parent package that keeps klib names unique.
  Rejected: keeping the per-path group.
- **Published kinds**:
  - Published: `COMMON`, `MODEL`, `SECURITY`, `INFRA`, `DI`, `DOMAIN`, `DATA`. This includes the skeleton modules
    (`core:model`, `core:network`, `core:database`): they are production modules, and `core:di` already declares
    some of them.
  - Never published: `FAKE`, `TESTING` (task.md out of scope), and any `apps/*` kind.
- **Version in the catalog**: `[versions] shared-core = "0.1.0-SNAPSHOT"`. Every version lives in
  `libs.versions.toml` (CLAUDE.md).
- **Repository `build/maven-repo` at the repo root**, named `local` (`publish…ToLocalRepository`). It is already
  git-ignored by `build/`, and a root build directory is not deleted by module `clean` tasks.
- **One command `publishSharedCore`**:
  - It is registered in each published module and depends on `publishAllPublicationsToLocalRepository`.
  - Running `./gradlew publishSharedCore` from the root runs it in every published module.
  - Using a task name in each module, instead of a root task that lists the projects, keeps the root build file
    free of cross-project wiring and keeps the configuration cache safe.
- **No sources or javadoc jars**: the consumer is in the same repo and reads the sources directly. Out of scope
  until remote publishing.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Module type from path | `moduleTypeOf`, `ModuleKind` | `build-logic/.../ModuleType.kt` |
| Per-type configuration in one place | `ModuleConventionPlugin` (the same place Kover is applied by kind) | `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt` |
| Catalog access | `libs.version(...)` | `build-logic/.../ProjectExtensions.kt` |
| Config-cache-safe build logic | Pattern of `configureKover()` | `build-logic/.../Kover.kt` |
| build-logic unit tests (JUnit Jupiter) | `ModuleTypeTest` style | `build-logic/convention/src/test/...` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| catalog | `gradle/libs.versions.toml` | `shared-core` version |
| build-logic | `buildlogic/Publishing.kt` (new), `ModuleConventionPlugin.kt` (modify), `PublishingTest.kt` (new) | Publishing setup and its rules |
| docs | `CLAUDE.md`, `.claude/rules/build-logic.md`, subtask 01 | Publish command and the rule |

All published modules (`core:common`, `core:model`, `core:security`, `core:network`, `core:database`,
`core:datastore`, `core:di`, `service:settings:domain`, `service:settings:data`) are affected only through
`tmdb.module`; their build files don't change.

## Steps
### Step 1: Publishing rules and setup in build-logic  (AC-1, EC-3) ✅
- Files: `gradle/libs.versions.toml`, `build-logic/.../buildlogic/Publishing.kt` (new),
  `build-logic/convention/src/main/kotlin/ModuleConventionPlugin.kt`,
  `build-logic/convention/src/test/.../PublishingTest.kt` (new)
- What:
  - Catalog: `shared-core = "0.1.0-SNAPSHOT"` under `[versions]`.
  - `Publishing.kt`:
    - Constants: `PUBLISH_GROUP = "com.banhmilabs.tmdb"`, `LOCAL_REPOSITORY = "local"`,
      `MAVEN_REPO_DIR = "build/maven-repo"`.
    - `internal fun isPublished(type: ModuleType): Boolean`: `type.app == null` and `type.kind !in setOf(FAKE,
      TESTING)`.
    - `internal fun artifactIdOf(path: String): String`: `:service:settings:domain` →
      `service-settings-domain`.
    - `internal fun Project.configurePublishing()`:
      - Apply `maven-publish`.
      - Set the `local` repository at `rootDir.resolve(MAVEN_REPO_DIR)` (a `File`, so the configuration cache
        is safe).
      - For `publications.withType<MavenPublication>().configureEach`: `groupId = PUBLISH_GROUP`,
        `version = libs.version("shared-core")`, and `artifactId` = the KMP-generated id with its
        `project.name` prefix replaced by `artifactIdOf(path)`.
      - Register `publishSharedCore`, which depends on `publishAllPublicationsToLocalRepository`.
  - `ModuleConventionPlugin`: after `configureKotlinMultiplatform`, `if (isPublished(type)) configurePublishing()`.
- Pseudo-code (artifactId):
  ```
  // KMP ids: "<name>" (root), "<name>-android", "<name>-jvm", "<name>-iosarm64", ...
  artifactId = artifactIdOf(path) + artifactId.removePrefix(project.name)
  ```
- Unit tests: `PublishingTest` (JUnit Jupiter):
  - `productionCoreAndServiceModulesArePublished`: common, model, security, infra, di, domain, data.
  - `testOnlyModulesAreNotPublished`: fake, testing.
  - `appModulesAreNotPublished`: cmp designsystem, ui, composeApp, androidApp.
  - `artifactIdJoinsThePathWithDashes`: `:core:di` → `core-di`, `:service:settings:domain` →
    `service-settings-domain`.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin`, then `./gradlew :core:di:tasks --all` to
  confirm that `publishSharedCore` and `publishAllPublicationsToLocalRepository` exist (build-logic rule) and
  that `:service:settings:fake:tasks --all` has no `publishSharedCore`.

### Step 2: Docs ✅
- Files: `CLAUDE.md`, `.claude/rules/build-logic.md`
- What:
  - **`CLAUDE.md` "Commands"**:
    - Add the publish command: `./gradlew publishSharedCore` writes the shared core, at version
      `0.1.0-SNAPSHOT`, to `build/maven-repo`.
    - The published modules are the production modules of `core/` and `service/`, with coordinates
      `com.banhmilabs.tmdb:<path-with-dashes>`.
    - All targets; the iOS klibs need macOS + Xcode.
  - **`build-logic.md`**: `configurePublishing()`, `isPublished`, the coordinates, and the rule that test-only
    and app modules are never published.
- Unit tests: none.
- Compile: none (docs only).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 production modules published with POM at `0.1.0-SNAPSHOT` | 1 | `PublishingTest.productionCoreAndServiceModulesArePublished`, `artifactIdJoinsThePathWithDashes`; manual: run `./gradlew publishSharedCore`, list `build/maven-repo/com/banhmilabs/tmdb/`, open `service-settings-data-android-0.1.0-SNAPSHOT.pom` and see `service-settings-domain` and Koin as dependencies (subtask "How to test" 1–2) |
| AC-1 test-only modules not published | 1 | `PublishingTest.testOnlyModulesAreNotPublished`, `appModulesAreNotPublished`; manual: no `service-settings-fake` / `core-testing` in the repo (subtask "How to test" 3) |
| EC-3 republish replaces at the same version | 1 | Manual: change a shared module and publish again; the files at `0.1.0-SNAPSHOT` are replaced (subtask "How to test" 4) |

## Out of scope
- The `native-android/` consumer → 02-standalone-native-android-app.
- Architecture docs about the standalone app → 03-architecture-without-apps-native.
- Remote publishing, signing, sources/javadoc jars, and a version scheme beyond the fixed SNAPSHOT.

## Risks
- **Customised artifactIds and inter-module dependencies**: the POM / Gradle metadata of a published module must
  name its project dependencies with their customised coordinates (`com.banhmilabs.tmdb:service-settings-domain`,
  not `com.banhmilabs.tmdb.service.settings:domain`). Gradle takes these from the target project's publications.
  Mitigation: the manual POM check in Traceability. If the coordinates come out wrong, also set
  `project.group = PUBLISH_GROUP` for published modules only. The klib uniqueness would then come from the
  artifactId-based `archivesName` (deviation, ask first).
- **iOS publishing time and the macOS requirement**: publishing builds every iOS klib, so it is slow (minutes) and
  needs Xcode. Mitigation: run it in the background; on a machine without Xcode the iOS targets are skipped by the
  existing `kotlin.native.ignoreDisabledTargets`.
- **The AGP KMP library plugin's Android publication**: the Android target of
  `com.android.kotlin.multiplatform.library` must produce an `-android` publication with an AAR. Mitigation: check
  the `.aar` in the repo during the manual check. If it is missing, the Android target needs explicit publishing
  configuration (deviation).

## Deviations
- Skeleton modules with no production Kotlin sources (`core:model`, `core:network`, `core:database`) are not
  published yet (from user). `publishSharedCore` failed on them: an empty source set produces no iOS klib, so
  `generateMetadataFileForIosArm64Publication` found no file. `tmdb.module` now publishes a module only when
  `isPublished(type)` holds and the module has a `*Main` source set with `.kt` files (`hasProductionSources()`).
  Such a module is published automatically once it gets code. No module depends on these skeletons, so no POM
  points at a missing artifact.
