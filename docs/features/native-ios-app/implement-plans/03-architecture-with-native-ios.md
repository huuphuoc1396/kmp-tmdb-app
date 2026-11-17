# Plan: Architecture with native-ios

Requirement: [03 - Architecture with native-ios](../subtasks/03-architecture-with-native-ios.md)
Status: Done

## Summary
Docs-only change. `docs/architecture.md`, `CLAUDE.md` and `.claude/rules/testing.md` still describe `native-ios/` as
planned. Replace those notes with the setup built in 01 and 02:
- the `core:ios` module and its `SharedCore` entry point,
- the `TmdbCore` XCFramework delivered as the local Swift Package `build/swift-package/TmdbCore`,
- SKIE interop,
- the Xcode project, and the commands to build and test it.

AC-6 is checked by confirming that no Swift sources are in the KMP build and by running the CI checks once.

## Decisions
- **Swift ViewModels use `ObservableObject` with `@Published`**, held with `@StateObject` in the view. This keeps the
  iOS 16 minimum shared with the KMP build. User's choice; rejected: raising the minimum to iOS 17 for `@Observable`.
  The open question in `docs/features/main-skeleton/task.md` is answered here and noted there.
- **The native iOS testing row mirrors native Android.** XCTest runs in the app's test host against the real shared
  core through the published API. There are no Swift fakes, because Kotlin rejects a `Flow` implemented in Swift. UI
  is checked manually on the simulator, and XCUITest is dropped. User's choice; rejected: keeping XCUITest for flows.
- **The `testing.md` Keychain note is updated, not removed.** Kotlin `iosSimulatorArm64Test` still has no app bundle.
  `native-ios/` tests run in an app test host, so the Keychain-backed settings path runs end to end there, through
  the published framework. `core:security`'s Kotlin tests still don't run on iOS.
- **`core:ios` gets its own entries in the architecture doc's module sections 5.1–5.3.** The doc has no entry for it
  today; only `CLAUDE.md` and `build-logic.md` describe it.
- **AC-6 needs no code change.** A search found no `.swift` file outside `native-ios/` and `apps/cmp/iosApp/`, and
  `ci.yml` has no native step.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Wording for the standalone app | `native-android/` subsection, CI block | `docs/architecture.md` sections 11 and 13 |
| Commands to build and test | Native iOS README | `native-ios/README.md` |
| iOS SDK build facts | `configureIosSdk()` description | `.claude/rules/build-logic.md` |
| Dependency rule for `core:ios` | `IOS_SDK` rule | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/DependencyRules.kt:36,45` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| docs | `docs/architecture.md` (modify) | Describe `native-ios/`, `core:ios` and iOS publishing; drop the "planned" notes |
| docs | `CLAUDE.md` (modify) | Intro, repository layout, native iOS commands |
| rules | `.claude/rules/testing.md` (modify) | Keychain limitation note |
| docs | `docs/features/main-skeleton/task.md` (modify) | Close the `@Observable` open question |

## Steps
### Step 1: architecture.md (AC-6) ✅
- Files: `docs/architecture.md` (modify)
- What:
  - **§1 Goals table:** the `native-ios/` row becomes "published XCFramework (local Swift Package) | shared | Swift
    `ObservableObject` | SwiftUI". Drop "(planned)".
  - **§2 layout:** the `native-ios/` comment becomes "Standalone Xcode project: native iOS app on the published
    core". Add a bullet: `native-ios/` has its own committed Xcode project and reaches the core only through the
    Swift Package `build/swift-package/TmdbCore` (section 14), so it sees a core change after the core is published
    again.
  - **§5.1 tree:** add `ios/` under `core/`, commented "iOS entry point SharedCore; builds the TmdbCore
    XCFramework".
  - **§5.2 table:** add the row `core/ios` | ios-sdk | "`SharedCore` (start, repositories); exports its `api`
    dependencies (kernel, service domains) in the XCFramework".
  - **§5.3 table:** add the row "ios-sdk | everything in the shared core; no module depends on it".
  - **§5.3 forbidden list:** add "anything → `core:ios`".
  - **§8 testing row:** "Native iOS (`native-ios/`) | XCTest in the app's test host against the real shared core
    through the published API (Swift can't implement a Kotlin `Flow`, so no Swift fakes); UI checked manually on the
    simulator | simulator, run locally".
  - **§11 `native-ios/` subsection** (drop "(planned)"):
    - A layout block: `TmdbNative.xcodeproj` (committed, file-system synchronized groups, shared scheme),
      `TmdbNative/` (App, views, `Theme`, `Info.plist`, assets), `TmdbNativeTests/`.
    - Bullets:
      - One app target to start; feature folders are split out when a second feature needs them.
      - Dependency: the local package `../build/swift-package/TmdbCore`, product `TmdbCore`, that links one
        umbrella framework.
      - Start-up: `SharedCore.shared.start()` in the `App` init. Repositories are read through `SharedCore`'s
        accessors, and Swift never sees Koin.
      - Interop (SKIE): `Flow<T>` → `SkieSwiftFlow<T>`, an `AsyncSequence` read with `for await`; `suspend` →
        `async throws`; sealed types → `onEnum(of:)` for an exhaustive `switch`.
      - ViewModels are `@MainActor` `ObservableObject`s with `@Published` state, held with `@StateObject`. The
        feature rules are the same as in the other apps.
      - Workflow: `./gradlew publishSharedCore` at the root, then build in `native-ios/` (see its `README.md`).
  - **§11 Localization:** the `native-ios/` UI row drops "(planned)".
  - **§12 tech stack:**
    - The UI row drops "planned".
    - The iOS interop row becomes "SKIE (in the `TmdbCore` XCFramework)".
  - **§13 CI:** add a block after native-android: `native-ios/` is not built on CI; publish, then `xcodebuild … build`
    and `xcodebuild … test` from `native-ios/` (the commands in its README).
  - **§14:**
    - Add an **iOS** bullet:
      - On macOS, `publishSharedCore` also builds `core:ios` into a static release XCFramework `TmdbCore`
        (`iosArm64`, `iosSimulatorArm64`, with SKIE).
      - It writes the XCFramework with a generated `Package.swift` (iOS major from the catalog) to the git-ignored
        `build/swift-package/TmdbCore`.
      - `core:ios` is not published to Maven.
      - A consumer adds it as a local package. A build before publishing fails with "the package … cannot be
        accessed".
    - Mention `build-logic/.../buildlogic/IosSdk.kt` next to `Publishing.kt`.
- Unit tests: none (docs).
- Compile: none (docs).

### Step 2: CLAUDE.md, testing rule, open question (AC-6) ✅
- Files: `CLAUDE.md`, `.claude/rules/testing.md`, `docs/features/main-skeleton/task.md` (modify)
- What:
  - **`CLAUDE.md`:**
    - **Intro:** "…the standalone native apps `native-android/` and `native-ios/`".
    - **Repository layout:** add a `native-ios/` bullet. It is a standalone Xcode project (SwiftUI, iOS 16, one app
      target plus a hosted test target) that is not part of the KMP build. It uses the shared core only through the
      Swift Package `build/swift-package/TmdbCore` (`SharedCore` entry point, SKIE). See `native-ios/README.md`.
    - **Commands:** add a "Native iOS app" line: `./gradlew publishSharedCore`, then from `native-ios/` the
      `xcodebuild … build` command and the `xcodebuild … -destination 'platform=iOS Simulator,name=iPhone 17' test`
      command. Not run by CI.
  - **`testing.md`:** reword the Keychain limitation. The Kotlin `iosSimulatorArm64Test` has no app bundle, so
    `core:security`'s Keychain tests are verified on JVM and Android only. The `native-ios/` tests run in an app
    test host and exercise the Keychain-backed settings path end to end through the published framework.
  - **`main-skeleton/task.md`:** tick the `@Observable` open question, with the answer "`ObservableObject` (iOS 16
    kept), decided in native-ios-app 03".
- Unit tests: none.
- Compile: none.

### Step 3: Verify AC-6 ✅
- What:
  - `find . -name '*.swift' -not -path './native-ios/*' -not -path './apps/cmp/iosApp/*' -not -path '*/build/*'`
    returns nothing.
  - `grep -rn "native-ios" settings.gradle.kts build-logic .github` returns nothing.
  - Run `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` once and confirm it passes.
  - Run `grep -rn "planned" docs/architecture.md CLAUDE.md`: no `native-ios` or SKIE match remains.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-6 | 1, 2, 3 | The Step 3 search (no Swift in the KMP build, no `native-ios` reference in Gradle or CI) and the CI checks passing |

## Out of scope
- The onboarding screen on iOS: onboarding subtask 07, which follows the `ObservableObject` decision here.
- Any code or build change: none is needed.

## Risks
- The README commands could drift from the docs. Mitigation: `CLAUDE.md` and §13 repeat the README's commands
  exactly, and §11 points to the README for details.

## Deviations
- None.

## Results
- No `.swift` file outside `native-ios/` and `apps/cmp/iosApp/`; no `native-ios` reference in `settings.gradle.kts`,
  `build-logic` or `.github`; no "planned" note about native iOS or SKIE left in `docs/architecture.md` or `CLAUDE.md`.
- `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue`: passed (exit 0), 2026-11-16.
