# Plan: Standalone native Android app

Requirement: [02 - Standalone native Android app](../subtasks/02-standalone-native-android-app.md)
Status: Done

## Summary
`native-android/` is a new, self-contained Gradle project with one `:app` module. It has its own settings, wrapper,
version catalog and Detekt config, and resolves the shared core only from `../build/maven-repo` (published by
`./gradlew publishSharedCore`, subtask 01). The app shell reuses the code of the closed PR #11 (theme, Activity,
NavHost, placeholder Home) in the package `com.banhmilabs.tmdb.nativeapp`. It starts the shared core with
`initKoin` from the published `core-di`. A smoke unit test uses the published `SettingsRepository` API with MockK. A
README documents publish-then-build.

## Decisions
- **One `:app` module** (from user). Theme, Activity, navigation and the placeholder Home live in it, with the theme
  in its own package `…nativeapp.designsystem`. Modules are split out when a second feature needs them. Rejected:
  `:app` + `:designsystem` now.
- **AC-4 test is a smoke test of the published domain API** (from user):
  - What it does: it mocks `SettingsRepository` (from the `service-settings-domain` AAR) with MockK and reads
    `observeOnboardingCompleted()`.
  - What it proves: the published API resolves and is usable from the app's unit tests.
  - Why not the real repository: it needs the Android Keystore, which a JVM unit test doesn't have. No production
    code is added just for the test.
  - Rejected: an `AppViewModel` before onboarding needs it.
  - "The shared core is started without errors" is checked by launching the app.
- **Shared core dependencies**:
  - `com.banhmilabs.tmdb:core-di` for `initKoin`. It brings `service-settings-data`, `core-datastore` and
    `core-security` transitively, as published in 01.
  - `com.banhmilabs.tmdb:service-settings-domain` for the domain API.
  - Both are declared by their root coordinates; Gradle metadata selects the `-android` variant.
- **Repository**:
  - `native-android/settings.gradle.kts` declares `maven(url = "../build/maven-repo")`, restricted to group
    `com.banhmilabs.tmdb` with `content { includeGroup(...) }` (EC-1: a clear "could not find
    com.banhmilabs.tmdb:core-di" when nothing is published).
  - `google()` and `mavenCentral()` exclude that group, so a missing publish never falls back to a remote search.
  - There is no `includeBuild`, no `project(...)` and no source path into the KMP build (AC-2).
- **Package and IDs**: package `com.banhmilabs.tmdb.nativeapp`, namespace and applicationId
  `com.banhmilabs.tmdb.nativeapp`, label "TMDB Native". These differ from the cmp app (`com.banhmilabs.tmdb.cmp`,
  "TMDB"), so the two install side by side (AC-5). The standalone project has no `native` path segment to alias.
- **Same toolchain as the KMP build, copied rather than shared** (task.md: nothing shared):
  - Gradle 9.9.0 wrapper, AGP 9.5.1 (built-in Kotlin), Kotlin 2.4.21, JDK 21.
  - compileSdk / targetSdk 37, minSdk 24.
  - Jetpack Compose BOM 2026.09.00, Navigation Compose 2.10.2, activity-compose 1.13.0, Koin 4.2.2,
    kotlinx-serialization 1.12.0.
  - Kotlin must be at least the version the AARs were compiled with (2.4.21), or reading their metadata fails.
- **Detekt**:
  - The `dev.detekt` plugin and the ktlint wrapper rules go on `:app`.
  - The config `native-android/config/detekt/detekt.yml` is a copy of the KMP build's config (line length 120,
    `FunctionNaming` ignores `@Composable`).
- **Unit tests**: JUnit 4 through `kotlin-test-junit`, MockK and `kotlinx-coroutines-test` on
  `testImplementation`, run with `testDebugUnitTest`. These are the same tools as the KMP build's JVM tests.
- **No comments in new code** (user preference, recorded in PR #9). The README carries the explanations.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Theme values and shell code | `Theme.kt`, `MainActivity`, `AppNavHost`, `HomeRoute`, `PlaceholderHomeScreen`, `TmdbApplication` from the closed PR #11 | branch `native-android-app-shell`, `apps/native/...` (copied, repackaged) |
| Edge-to-edge with light bars | `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT))` | same, and `apps/cmp/androidApp/.../MainActivity.kt` |
| Koin start-up | `initKoin(appDeclaration)` | published `com.banhmilabs.tmdb:core-di` |
| Onboarding state API | `SettingsRepository.observeOnboardingCompleted()` | published `com.banhmilabs.tmdb:service-settings-domain` |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/*` | repo root (copied) |
| Detekt config | `config/detekt/detekt.yml` | repo root (copied) |
| Versions | `gradle/libs.versions.toml` | repo root (entries copied) |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android/` (project) | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, `config/detekt/detekt.yml`, `README.md` (all new) | Standalone build |
| `native-android/app` | `build.gradle.kts`, `src/main/AndroidManifest.xml`, `src/main/kotlin/com/banhmilabs/tmdb/nativeapp/{TmdbApplication, MainActivity, AppNavHost, HomeRoute, PlaceholderHomeScreen}.kt`, `…/nativeapp/designsystem/Theme.kt`, `src/test/kotlin/com/banhmilabs/tmdb/nativeapp/SharedCoreApiTest.kt` (all new) | The app and its test |
| docs | subtask 02 | Status |

The KMP build is not touched.

## Steps
### Step 1: Standalone Gradle project skeleton  (AC-2, AC-7, EC-1) ✅
- Files:
  - `native-android/settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`
  - `native-android/gradle/wrapper/gradle-wrapper.{jar,properties}`, `gradlew`, `gradlew.bat` (copied from the
    root)
  - `native-android/config/detekt/detekt.yml` (copied)
- What:
  - **`settings.gradle.kts`**:
    - `pluginManagement` repositories: google, mavenCentral, gradlePluginPortal.
    - `dependencyResolutionManagement` with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`:
      - `maven("../build/maven-repo") { name = "sharedCore"; content { includeGroup("com.banhmilabs.tmdb") } }`.
      - `google()` and `mavenCentral()`, each with `content { excludeGroup("com.banhmilabs.tmdb") }`.
    - `rootProject.name = "native-android"`, `include(":app")`.
  - **Root `build.gradle.kts`**: plugins `apply false`: android-application, kotlin-compose, kotlin-serialization,
    detekt.
  - **`gradle.properties`**: `org.gradle.jvmargs=-Xmx4g`, `org.gradle.configuration-cache=true`,
    `org.gradle.caching=true`, `android.useAndroidX=true`, `kotlin.code.style=official`.
  - **Catalog**:
    - Versions: agp 9.5.1, kotlin 2.4.21, detekt 2.0.0-alpha.6, jvmToolchain 21, compileSdk 37, targetSdk 37,
      minSdk 24, compose-bom 2026.09.00, navigation 2.10.2, activity 1.13.0, koin 4.2.2, serialization 1.12.0,
      mockk 1.14.12, coroutines 1.11.0, shared-core 0.1.0-SNAPSHOT.
    - Libraries: `shared-core-di`, `shared-core-settings-domain`, `compose-bom`, `compose-ui`,
      `compose-foundation`, `compose-material3`, `navigation-compose`, `activity-compose`, `koin-android`,
      `kotlinx-serialization-core`, `detekt-rules-ktlint-wrapper`, `kotlin-test-junit`, `mockk`,
      `kotlinx-coroutines-test`.
    - Plugins: the four above.
- Unit tests: none (skeleton).
- Compile: none on its own. `include(":app")` needs the `app/` folder from step 2, so the skeleton is checked by
  step 2's `assembleDebug`, after `./gradlew publishSharedCore` at the repo root.

### Step 2: The app shell  (AC-3, AC-4, AC-5) ✅
- Files:
  - `native-android/app/build.gradle.kts`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/`: `TmdbApplication.kt`, `MainActivity.kt`,
    `AppNavHost.kt`, `HomeRoute.kt`, `PlaceholderHomeScreen.kt`, `designsystem/Theme.kt`
- What:
  - **`app/build.gradle.kts`**:
    - Plugins: android-application, kotlin-compose, kotlin-serialization, detekt.
    - `android { namespace / applicationId = "com.banhmilabs.tmdb.nativeapp", compileSdk, minSdk, targetSdk,
      versionCode 1, versionName "0.1.0", buildFeatures.compose = true }`, `kotlin { jvmToolchain(21) }`.
    - `detekt { buildUponDefaultConfig = true; config.setFrom(rootProject.file("config/detekt/detekt.yml")) }`.
    - Dependencies:
      - `implementation`: `shared-core-di`, `shared-core-settings-domain`, `platform(compose-bom)`, compose ui /
        foundation / material3, `navigation-compose`, `activity-compose`, `koin-android`,
        `kotlinx-serialization-core`.
      - `detektPlugins(detekt-rules-ktlint-wrapper)`.
  - **Manifest**: `.TmdbApplication`, launcher `.MainActivity`, label "TMDB Native", theme
    `@android:style/Theme.Material.NoActionBar`.
  - **Kotlin code**: the same as PR #11, repackaged to `com.banhmilabs.tmdb.nativeapp`.
    - `TmdbApplication`: `initKoin { androidContext(this@TmdbApplication) }`.
    - `MainActivity`: `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT) ×2)`, then
      `setContent { TmdbTheme { AppNavHost() } }`.
    - `@Serializable internal data object HomeRoute`.
    - `AppNavHost(navController = rememberNavController())` starts at `HomeRoute` and shows
      `PlaceholderHomeScreen`.
    - `TmdbTheme`: the cmp colours (`0xFF8000FF`, `0xFF540BA1`, `0xFF15151D`).
- Unit tests: none in this step (step 3).
- Compile: `cd native-android && ./gradlew :app:assembleDebug`

### Step 3: Smoke test of the published API  (AC-4, AC-7) ✅
- Files: `native-android/app/src/test/kotlin/com/banhmilabs/tmdb/nativeapp/SharedCoreApiTest.kt`;
  `app/build.gradle.kts` (test dependencies)
- What:
  - `testImplementation`: `kotlin-test-junit`, `mockk`, `kotlinx-coroutines-test`.
  - `SharedCoreApiTest`:
    - `readsOnboardingCompletedThroughPublishedDomainApi`:
      `mockk<SettingsRepository> { every { observeOnboardingCompleted() } returns flowOf(true) }`, then
      `runTest { assertTrue(repository.observeOnboardingCompleted().first()) }`.
    - `readsNotCompletedThroughPublishedDomainApi`: the same with `false`.
- Compile: `cd native-android && ./gradlew :app:compileDebugUnitTestKotlin`

### Step 4: README  (EC-1, EC-2) ✅
- Files: `native-android/README.md`
- What:
  - Purpose: a standalone native Android app that consumes the shared core like an SDK.
  - Prerequisites: JDK 21, Android SDK via `native-android/local.properties` (`sdk.dir`).
  - Workflow:
    1. `./gradlew publishSharedCore` at the repo root.
    2. `cd native-android && ./gradlew :app:installDebug`.
    3. After changing the shared core, publish again and rebuild.
  - Checks: `./gradlew :app:detekt :app:testDebugUnitTest`.
  - Troubleshooting: "Could not find com.banhmilabs.tmdb:…" means the shared core was not published (EC-1). An
    old behaviour after a core change means it was not republished (EC-2).
- Unit tests: none.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-2 builds on its own from the in-repo repository | 1, 2 | `./gradlew :app:assembleDebug` in `native-android/`; review: no `includeBuild` / `project(...)` / path into the KMP build in its Gradle files (subtask "How to test" 1) |
| AC-3 placeholder Home, dark, edge-to-edge, light bars | 2 | Manual: emulator launch and screenshot (subtask "How to test" 2) |
| AC-4 shared core started; domain API readable | 2, 3 | Manual launch without crash; `SharedCoreApiTest.readsOnboardingCompletedThroughPublishedDomainApi`, `readsNotCompletedThroughPublishedDomainApi` |
| AC-5 installs next to the cmp app | 2 | Manual: install both and launch both (subtask "How to test" 3) |
| AC-7 local static analysis and unit tests | 1, 3 | `./gradlew :app:detekt :app:testDebugUnitTest` pass (subtask "How to test" 4) |
| EC-1 build before publishing fails clearly | 1, 4 | Manual: move `build/maven-repo` aside and build; "Could not find com.banhmilabs.tmdb:core-di"; README names the command (subtask "How to test" 5) |
| EC-2 stale shared core until republished | 4 | Manual (subtask "How to test" 6) |

## Out of scope
- KMP build docs, the project guide and rules (`native-android/` in `docs/architecture.md`, `CLAUDE.md`) →
  03-architecture-without-apps-native.
- Onboarding in this app → onboarding subtask 06.
- CI, app icon, real Home.

## Risks
- **SNAPSHOT caching**: Gradle normally caches changing modules for 24h. A local file repository is read directly
  rather than cached, so a republish is visible on the next build. Mitigation: check EC-2 manually. If a stale
  artifact is used, add `resolutionStrategy.cacheChangingModulesFor(0, "seconds")` to `:app` (deviation).
- **AAR + Gradle metadata variant selection**: depending on the root coordinates relies on the published
  `.module` files that point at `-android`. Mitigation: `assembleDebug` in step 2. If selection fails, depend on
  the `-android` coordinates directly (deviation).
- **Transitive runtime-only dependencies**: `core-di` depends on `service-settings-data` with runtime scope, so
  the data classes are on the runtime but not the compile classpath. This is the intended SDK surface. Mitigation:
  the app launch verifies the runtime graph (Koin, DataStore, Keystore).
- **Wrapper JAR in a second place**: the copied `gradle-wrapper.jar` is a binary in the repo. It is the same file
  as the root's, so it adds no new trust issue.

## Deviations
- `app/build.gradle.kts` also sets `detekt { source.setFrom(layout.projectDirectory.dir("src")) }`, as the KMP
  build's `tmdb.detekt` does, so Detekt covers both `src/main` and `src/test`.
