# Plan: Publish shared core for iOS

Requirement: [01 - Publish shared core for iOS](../subtasks/01-publish-shared-core-ios.md)
Status: Done

## Summary
A new shared module, `:core:ios`, of a new kind `IOS_SDK`, is the iOS entry point of the shared core.
- It holds a Koin-free `SharedCore` object that starts the core and returns the domain's repositories.
- It exports the shared kernel and the `service/*/domain` modules, which are its `api` dependencies, with SKIE for
  Swift-friendly interop.
- It builds a static release XCFramework named `TmdbCore` with the `iosArm64` and `iosSimulatorArm64` slices.

`./gradlew publishSharedCore` now also packages that XCFramework with a generated `Package.swift` into the
git-ignored folder `build/swift-package/TmdbCore/` at the repo root (from user: one command). The folder is
replaced on every publish. Build-logic rules make sure that nothing depends on `:core:ios` and that it is never
published to Maven.

## Decisions
- **Module `:core:ios`, kind `IOS_SDK`** (from user):
  - It is a composition root for iOS, like `core:di`: it may depend on anything in the shared core. It never
    depends on an app, and no module may depend on it.
  - Konsist and the derived namespace (`com.banhmilabs.tmdb.core.ios`) pick it up from its `core/<name>` path.
  - Rejected: putting the entry point in `core:di`; a new `:sdk:ios` tier.
- **One command** (from user): `publishSharedCore` in `:core:ios` depends on the packaging task, so
  `./gradlew publishSharedCore` writes both the Maven repository and the Swift Package. On a host without macOS,
  the task in `:core:ios` has no dependency (the iOS targets are disabled there by
  `kotlin.native.ignoreDisabledTargets`), so the command still works for Android-only work. Rejected: a separate
  `publishSharedCoreIos`.
- **Framework and package name `TmdbCore`** (from user): Swift writes `import TmdbCore`.
- **Entry point type `SharedCore`, not `TmdbCore`**. A Swift type with the same name as its module breaks
  qualified names in Swift, so the object gets another name.
  ```kotlin
  object SharedCore {
      fun start()                                  // idempotent; calls initKoin() once
      val settingsRepository: SettingsRepository   // fails with a clear message before start()
  }
  ```
  - It keeps the `Koin` instance returned by `initKoin()` in a private field. It does not implement
    `KoinComponent`, which would expose Koin types in the Swift API.
  - Swift reaches it as `SharedCore.shared`.
- **Exports**: every `api` project dependency of `:core:ios`'s `commonMain`. Each framework's export configuration
  extends `commonMainApi`, so the export list is the module's own dependency block and needs no hardcoded paths in
  build-logic.
  - Exported now: `api(projects.core.common)` and `api(projects.service.settings.domain)`.
  - Internal: `implementation(projects.core.di)`.
  - `core:model` is not exported yet: it has no code, and an empty module produces no klib (native-android-app
    01). It is added when it gets code.
- **SKIE 0.10.16**, applied only to `:core:ios`. It is the latest release, and its Gradle plugin lists Kotlin
  2.4.21 as supported. It brings `Flow` → `AsyncSequence`, `suspend` → `async throws`, and sealed classes or enums
  → Swift enums. SKIE uses its default configuration.
- **Static release framework**, with only the `iosArm64` and `iosSimulatorArm64` slices (task.md). Static means
  nothing to embed, as for the cmp `ComposeApp` framework.
- **Package location `build/swift-package/TmdbCore/`** at the repo root, next to `build/maven-repo`. It is
  git-ignored by `build/` and not deleted by module `clean` tasks.
  - It holds `Package.swift` and `TmdbCore.xcframework`.
  - The manifest uses swift-tools 5.9, platform `.iOS(.v16)` (the major version of `ios-deploymentTarget` from
    the catalog), one library product `TmdbCore`, and a `binaryTarget(name: "TmdbCore", path:
    "TmdbCore.xcframework")`.
- **Republish replaces the package (EC-3)**: the packaging task is a `Sync`, so stale files are removed.
- **Not published to Maven**: `IOS_SDK` joins the unpublished kinds. Android consumers use `core-di` and the
  domain artifacts directly.
- **Tests**: `SharedCoreTest` in `jvmTest`, where Koin and the settings data layer already start
  (`InitKoinTest`). The framework contents are checked manually on macOS.
- **Docs in this subtask**: the publish command in `CLAUDE.md`, the `core:ios` row in its module table, and the
  build-logic rule. The architecture doc and the rest are subtask 03.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Start the shared core | `initKoin()` | `core/di/src/commonMain/kotlin/com/banhmilabs/tmdb/core/di/InitKoin.kt` |
| iOS framework setup | `configureKotlinMultiplatform(iosFramework)` (static framework for app shells) | `build-logic/.../buildlogic/KotlinMultiplatform.kt` |
| Module type from path | `moduleTypeOf`, `CORE_KINDS` | `build-logic/.../buildlogic/ModuleType.kt` |
| Dependency rules | `DependencyRules.violation`, `ruleFor` | `build-logic/.../buildlogic/DependencyRules.kt` |
| Publish command and exclusions | `publishSharedCore`, `UNPUBLISHED_KINDS`, `isPublished` | `build-logic/.../buildlogic/Publishing.kt` |
| Koin test shape | `InitKoinTest` (`stopKoin` in `@AfterTest`) | `core/di/src/jvmTest/...` |
| Config-cache-safe build logic | `configurePublishing()` / `configureKover()` pattern | `build-logic/.../buildlogic/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| catalog / root | `gradle/libs.versions.toml`, `build.gradle.kts` (modify) | SKIE version, plugin and Gradle plugin library; `apply false` |
| build-logic | `ModuleType.kt`, `DependencyRules.kt`, `Publishing.kt`, `ModuleConventionPlugin.kt` (modify); `buildlogic/IosSdk.kt` (new); `convention/build.gradle.kts` (modify); tests `ModuleTypeTest`, `DependencyRulesTest`, `PublishingTest` (modify) | New kind, its rules, XCFramework + Swift Package |
| `core:ios` (new) | `core/ios/build.gradle.kts`, `src/commonMain/.../core/ios/SharedCore.kt`, `src/jvmTest/.../core/ios/SharedCoreTest.kt`; `settings.gradle.kts` (modify) | The iOS entry point |
| docs | `CLAUDE.md`, `.claude/rules/build-logic.md`, subtask 01 | Command, module table, rule |

No existing module's public API changes.

## Steps
### Step 1: The `IOS_SDK` module kind and its rules  (AC-1) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/{ModuleType,DependencyRules,Publishing}.kt`,
  `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/{ModuleTypeTest,DependencyRulesTest,PublishingTest}.kt`
- What:
  - `ModuleKind.IOS_SDK`; `CORE_KINDS["ios"] = IOS_SDK`.
  - `DependencyRules.violation`:
    - Return `"core:ios is an entry point for iOS apps; no module depends on it"` when `to.kind == IOS_SDK`. This
      check goes after `appViolation`.
    - `IOS_SDK` may depend on anything (`DI, IOS_SDK, APP, ANDROID_APP -> true`).
    - `ruleFor(IOS_SDK)` is "core:ios may depend on anything in the shared core".
  - `Publishing.kt`: `UNPUBLISHED_KINDS = setOf(FAKE, TESTING, IOS_SDK)`.
- Unit tests:
  - `ModuleTypeTest.coreModulesMapToTheirKinds`: `:core:ios` → `IOS_SDK`.
  - `DependencyRulesTest`:
    - `iosSdkMayDependOnSharedCore`: `:core:ios` → `:core:di`, `:service:settings:domain`, `:core:common`.
    - `nothingDependsOnIosSdk`: `:core:di` → `:core:ios` and `:apps:cmp:composeApp` → `:core:ios` are rejected.
    - `iosSdkNeverDependsOnAnApp`: `:core:ios` → `:apps:cmp:designsystem` is rejected.
  - `PublishingTest.testOnlyModulesAreNotPublished` is renamed `testOnlyAndIosModulesAreNotPublished` and adds
    `IOS_SDK`.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin`

### Step 2: XCFramework, Swift Package and SKIE in build-logic  (AC-1, EC-3) ✅
- Files: `gradle/libs.versions.toml`, root `build.gradle.kts`, `build-logic/convention/build.gradle.kts`,
  `buildlogic/IosSdk.kt` (new), `ModuleConventionPlugin.kt`
- What:
  - **Catalog**:
    - `[versions] skie = "0.10.16"`.
    - `[libraries] skie-gradlePlugin = { module = "co.touchlab.skie:gradle-plugin", version.ref = "skie" }`.
    - `[plugins] skie = { id = "co.touchlab.skie", version.ref = "skie" }`.
  - Root `build.gradle.kts`: `alias(libs.plugins.skie) apply false`. `build-logic`:
    `compileOnly(libs.skie.gradlePlugin)`.
  - **`IosSdk.kt`**:
    - Constants `IOS_SDK_NAME = "TmdbCore"`, `SWIFT_PACKAGE_DIR = "build/swift-package"`, `SWIFT_TOOLS = "5.9"`.
    - `internal fun Project.configureIosSdk()`:
      - Apply `co.touchlab.skie`.
      - Create `XCFramework(IOS_SDK_NAME)`.
      - For `iosArm64()` and `iosSimulatorArm64()`, add `binaries.framework { baseName = IOS_SDK_NAME; isStatic =
        true; xcf.add(this) }`.
      - Make each framework's `exportConfigurationName` configuration `extendsFrom(configurations["commonMainApi"])`.
      - Register `writeTmdbCorePackageManifest` (writes `Package.swift` from `swiftPackageManifest(iosMajor)`).
      - Register `syncTmdbCoreSwiftPackage` (`Sync`) into `rootDir/build/swift-package/TmdbCore`, from
        `assembleTmdbCoreReleaseXCFramework`'s output folder `TmdbCore.xcframework` and the manifest.
      - Register `publishSharedCore`, which depends on `syncTmdbCoreSwiftPackage` only when
        `HostManager.hostIsMac`.
    - `internal fun swiftPackageManifest(iosMajor: Int): String`: a pure function, so it can be unit tested.
  - `ModuleConventionPlugin`: `if (type.kind == ModuleKind.IOS_SDK) configureIosSdk()` after
    `configureKotlinMultiplatform()`.
  - Pseudo-code (manifest):
    ```
    // swift-tools-version:5.9
    import PackageDescription
    let package = Package(
        name: "TmdbCore",
        platforms: [.iOS(.v$iosMajor)],
        products: [.library(name: "TmdbCore", targets: ["TmdbCore"])],
        targets: [.binaryTarget(name: "TmdbCore", path: "TmdbCore.xcframework")]
    )
    ```
- Unit tests: `IosSdkTest.manifestDeclaresBinaryTargetAndPlatform`: it contains `.iOS(.v16)`,
  `binaryTarget(name: "TmdbCore", path: "TmdbCore.xcframework")` and the `TmdbCore` library product.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin`

### Step 3: The `:core:ios` module and `SharedCore`  (AC-1) ✅
- Files: `settings.gradle.kts` (add `":core:ios"`), `core/ios/build.gradle.kts`,
  `core/ios/src/commonMain/kotlin/com/banhmilabs/tmdb/core/ios/SharedCore.kt`,
  `core/ios/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/ios/SharedCoreTest.kt`
- What:
  - `build.gradle.kts`: `plugins { alias(libs.plugins.tmdb.module) }`.
    - `commonMain`: `api(projects.core.common)`, `api(projects.service.settings.domain)`,
      `implementation(projects.core.di)`.
    - `jvmTest`: nothing extra (`kotlin-test` and MockK come from `tmdb.module`).
  - `SharedCore` (commonMain):
    - `private var koin: Koin? = null`.
    - `fun start()`: `if (koin == null) koin = initKoin().koin`.
    - `val settingsRepository: SettingsRepository get() = checkNotNull(koin) { "Call SharedCore.start() first" }.get()`.
  - A KDoc on `SharedCore` for the Swift consumer (public API of a published SDK).
- Unit tests: `SharedCoreTest` (with `@AfterTest stopKoin()`):
  - `startProvidesSettingsRepository`.
  - `startTwiceKeepsTheSameCore`: two `start()` calls, no exception, the same repository instance.
  - `settingsRepositoryBeforeStartFails`: `assertFailsWith<IllegalStateException>`.

  The object keeps state between tests, so `SharedCore` gets an `internal fun reset()` used only by the tests'
  `@AfterTest`. The alternative was a class plus a singleton holder; `reset()` is smaller.
- Compile: `./gradlew :core:ios:compileTestKotlinJvm :core:ios:detekt`, then on macOS `./gradlew publishSharedCore`
  (in the background, it takes several minutes).

### Step 4: Docs  (AC-1) ✅
- Files: `CLAUDE.md`, `.claude/rules/build-logic.md`
- What:
  - **`CLAUDE.md`**:
    - **Commands**: "Publish the shared core" now also says it writes the iOS Swift Package
      `build/swift-package/TmdbCore` (a release static XCFramework `TmdbCore`, slices `iosArm64` and
      `iosSimulatorArm64`, with SKIE; macOS + Xcode). `core:ios` is not on Maven.
    - **Modules table**: add a row `core:ios` | "iOS entry point (`SharedCore`), exports the kernel and the service
      domains to the `TmdbCore` XCFramework" | "everything in the shared core; nothing depends on it".
  - **`build-logic.md`**:
    - Add `IOS_SDK` (`:core:ios`) to the module types.
    - `configureIosSdk()`: SKIE, the XCFramework, exports from `commonMainApi`, the Swift Package sync, and
      `publishSharedCore`.
    - The rule "no module depends on core:ios; it is not published to Maven".
- Unit tests: none.
- Compile: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 release XCFramework, 2 slices, Swift Package | 2, 3 | `IosSdkTest.manifestDeclaresBinaryTargetAndPlatform`; manual: `./gradlew publishSharedCore` on macOS, then `build/swift-package/TmdbCore/` holds `Package.swift` and `TmdbCore.xcframework` with `ios-arm64` and `ios-arm64-simulator` (subtask "How to test" 1) |
| AC-1 exposes the domain API and one entry point | 3 | `SharedCoreTest.startProvidesSettingsRepository`, `startTwiceKeepsTheSameCore`, `settingsRepositoryBeforeStartFails`; manual: `TmdbCore.framework/Headers/TmdbCore.h` (and SKIE's Swift files) list `SharedCore` and `SettingsRepository` (subtask "How to test" 2) |
| AC-1 no test-only or app code | 1, 3 | `DependencyRulesTest.nothingDependsOnIosSdk`, `iosSdkNeverDependsOnAnApp` (fakes are rejected outside tests by the existing rules); manual: no `Fake` or `apps` type in the header |
| EC-3 republish replaces | 2 | Manual: change a shared module, run `publishSharedCore` again, and the files are newer with no stale ones (`Sync`) (subtask "How to test" 3) |
| CI unaffected | 1–3 | `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` passes (subtask "How to test" 4) |

## Out of scope
- `native-ios/` and its use of the package → 02-standalone-native-ios-app.
- Architecture doc, testing strategy and the remaining docs about native iOS → 03-architecture-with-native-ios.
- Debug frameworks, `iosX64`, remote distribution, and exporting `core:model` (when it has code).
- SKIE configuration beyond its defaults.

## Risks
- **SKIE with Kotlin 2.4.21 and AGP 9.5.1**: the SKIE 0.10.16 Gradle plugin lists 2.4.21, but it has not been
  tried with this build. Mitigation: step 3's `publishSharedCore` on macOS is the check. If SKIE fails, stop and
  ask (large deviation). The options would then be KMP-NativeCoroutines (it has Kotlin 2.4 builds) or the default
  Objective-C interop.
- **Export configuration extending `commonMainApi`**: the Kotlin plugin may need the per-target `api`
  configurations instead. Mitigation: check the header in step 3. If needed, use `iosMainApi` or call
  `export(...)` from the resolved dependencies of `commonMainApi` (small deviation).
- **Build time**: a release XCFramework with two slices takes minutes, and `publishSharedCore` now always builds it
  on macOS. Mitigation: run it in the background. A separate command was rejected by the user, so this is the
  accepted cost.
- **Configuration cache**: `HostManager.hostIsMac` and the `Sync` / manifest tasks must not capture `Project`.
  Mitigation: providers and `rootDir` resolved at configuration time, as in `Publishing.kt`.

## Deviations
- SKIE analytics are turned off in `configureIosSdk()` (`analytics { enabled = false; disableUpload = true }`).
  SKIE uploads build analytics to Touchlab by default, and the first publish uploaded them once before this was
  set.
- `syncTmdbCoreSwiftPackage` copies from `build/XCFrameworks/release/TmdbCore.xcframework` (with `dependsOn` on the
  assemble task). The task output with an `include` filter did not match, so the first publish wrote only
  `Package.swift`.
- `writeTmdbCorePackageManifest` declares the manifest text as an input, so the task stays up to date correctly.

## Results
- `./gradlew publishSharedCore` (macOS): `build/swift-package/TmdbCore/` holds `Package.swift` and
  `TmdbCore.xcframework` with `ios-arm64` and `ios-arm64-simulator`. The header lists `SharedCore`,
  `SettingsRepository`, `AppResult` and `AppError`, and no fake or app type. SKIE generates `completeOnboarding()
  async throws` and the `SkieSwiftFlow` `AsyncSequence` bridge. `core-ios` is not in `build/maven-repo`.
- Calling `observeOnboardingCompleted()` as an async sequence from Swift is first compiled in subtask 02.
