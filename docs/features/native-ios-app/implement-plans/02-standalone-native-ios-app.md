# Plan: Standalone native iOS app

Requirement: [02 - Standalone native iOS app](../subtasks/02-standalone-native-ios-app.md)
Status: Done

## Summary
`native-ios/` is a new, self-contained Xcode project with a committed `TmdbNative.xcodeproj`, an app target
`TmdbNative` (SwiftUI, iOS 16, Swift 6) and a hosted unit test target `TmdbNativeTests`.
- Its only dependency is the local Swift Package `../build/swift-package/TmdbCore`, published by
  `./gradlew publishSharedCore` (subtask 01).
- The app calls `SharedCore.shared.start()` at launch and shows a dark, edge-to-edge placeholder Home with light
  status bar content.
- A Swift test double of `SettingsRepository` lets the tests read the onboarding completed state through the
  published API. The tests read the raw Kotlin `Flow` with SKIE's `collect(collector:)`.
- A README documents the publish-then-build workflow.

The shared core is not changed.

## Decisions
- **Read the raw `Flow` in tests; do not change the shared core** (from user). In the published framework, SKIE
  bridges `completeOnboarding()` to `async throws`, but `SettingsRepository.observeOnboardingCompleted()`, a
  method of an interface, stays a raw `Kotlinx_coroutines_coreFlow`.
  - The tests collect it with SKIE's `Flow.collect(collector:) async throws` and a small Swift collector.
  - How the app itself turns it into an `AsyncSequence` is decided in onboarding 07's blueprint.
  - Rejected: adding a Swift-friendly accessor to `core:ios` now.
- **Test double in Swift**:
  - `StubSettingsRepository` conforms to `SettingsRepository`. `observeOnboardingCompleted()` returns a
    `SingleValueFlow`, a Swift class conforming to `Kotlinx_coroutines_coreFlow` whose `collect` emits one
    `KotlinBoolean` and completes. `completeOnboarding()` returns `AppResultSuccess(value: KotlinUnit())`.
  - The name is `Stub…`, not `Fake…`: the project's `Fake*` classes are the Kotlin service fakes with a contract
    test.
  - Swift has no MockK. Published fakes are out of scope (task.md).
- **Xcode project with file-system-synchronized groups** (Xcode 16+, `objectVersion = 77`).
  - The `TmdbNative/` and `TmdbNativeTests/` folders are synchronized root groups, so adding a Swift file needs no
    project edit. The hand-written `project.pbxproj` stays small.
  - The installed Xcode is 26.4.1.
  - A shared scheme `TmdbNative` (build + test) is committed under `xcshareddata/xcschemes`, so `xcodebuild` works
    without opening Xcode.
  - Rejected: `objectVersion = 56` with explicit file references, as in `apps/cmp/iosApp`. It needs a project edit
    per file.
- **Package reference**: an `XCLocalSwiftPackageReference` with `relativePath = "../build/swift-package/TmdbCore"`.
  The product `TmdbCore` is linked to the app target only. The test target is hosted by the app (`TEST_HOST`,
  `BUNDLE_LOADER`), so the static Kotlin runtime is in the binary once.
  - When the package folder is missing, Xcode's package resolution fails and names the path (EC-1).
- **Targets and settings, copied from `apps/cmp/iosApp` where they apply**:
  - `IPHONEOS_DEPLOYMENT_TARGET = 16.0`, `SWIFT_VERSION = 6.0`, `TARGETED_DEVICE_FAMILY = 1,2`.
  - `PRODUCT_BUNDLE_IDENTIFIER = com.banhmilabs.tmdb.nativeapp`, with `.tests` for the test target.
  - `GENERATE_INFOPLIST_FILE = NO` with `TmdbNative/Info.plist`.
  - Signing: Automatic with no team ("Sign to Run Locally"), as in the cmp project. Simulator only.
- **Info.plist**:
  - `CFBundleDisplayName = TMDB Native`, `CFBundleShortVersionString = 0.1.0`, `CFBundleVersion = 1`.
  - `UILaunchScreen` → `UIColorName = LaunchBackground`; the asset is `#15151D`, the same as the app background.
  - All orientations, as in cmp.
  - `UIUserInterfaceStyle = Dark`, so system UI and the status bar are always light-on-dark.
  - `CFBundleLocalizations` is not added yet; the onboarding copy comes in 07.
- **Theme**: `Theme.swift` holds the app background `night` `#15151D` (the cmp / native-android value) as a
  SwiftUI `Color`. The brand purples come with onboarding (07), where they are first used.
  - Edge-to-edge: the root view draws `Theme.night.ignoresSafeArea()` behind the content.
- **Start-up**: `TmdbNativeApp.init()` calls `SharedCore.shared.start()` before the first scene, like
  `TmdbApplication.onCreate` on Android.
- **No comments in new code** (user preference); the README carries the explanations.
- **Swift files live at the target folder root** for now: `TmdbNativeApp.swift`, `PlaceholderHomeView.swift`,
  `Theme.swift`. Feature folders come with onboarding (07).

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Start the shared core, repository access | `SharedCore.shared.start()`, `.settingsRepository` | published `TmdbCore` (`core/ios/.../SharedCore.kt`) |
| Onboarding state API | `SettingsRepository.observeOnboardingCompleted()`, `completeOnboarding() async throws` | published `TmdbCore` (`service/settings/domain`) |
| Flow collection from Swift | `Kotlinx_coroutines_coreFlow.collect(collector:) async throws` (SKIE) | published `TmdbCore` |
| Project settings, Info.plist, launch colour | `iosApp.xcodeproj`, `iosApp/Info.plist`, `Assets.xcassets/LaunchBackground` | `apps/cmp/iosApp/` (copied values) |
| Colours | `TmdbTheme` values `0xFF15151D`, `0xFF8000FF`, `0xFF540BA1` | `native-android/app/.../designsystem/Theme.kt` |
| README shape | publish-then-build, checks, troubleshooting | `native-android/README.md` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios/` (project) | `TmdbNative.xcodeproj/project.pbxproj`, `TmdbNative.xcodeproj/xcshareddata/xcschemes/TmdbNative.xcscheme`, `README.md` (new) | Standalone project, shared scheme, docs |
| `native-ios/TmdbNative` | `TmdbNativeApp.swift`, `PlaceholderHomeView.swift`, `Theme.swift`, `Info.plist`, `Assets.xcassets/{Contents.json, LaunchBackground.colorset/Contents.json}` (new) | The app |
| `native-ios/TmdbNativeTests` | `SharedCoreApiTests.swift`, `StubSettingsRepository.swift` (new) | AC-4 test |
| docs | subtask 02 | Status |

The KMP build is not touched.

## Steps
### Step 1: Xcode project, app shell and launch screen  (AC-2, AC-3, AC-4, AC-5, EC-1) ✅
- Files: `native-ios/TmdbNative.xcodeproj/project.pbxproj`,
  `native-ios/TmdbNative.xcodeproj/xcshareddata/xcschemes/TmdbNative.xcscheme`,
  `native-ios/TmdbNative/{TmdbNativeApp.swift, PlaceholderHomeView.swift, Theme.swift, Info.plist}`,
  `native-ios/TmdbNative/Assets.xcassets/{Contents.json, LaunchBackground.colorset/Contents.json}`
- What:
  - **`project.pbxproj`** (`objectVersion = 77`):
    - A `PBXProject` with `packageReferences = [XCLocalSwiftPackageReference "../build/swift-package/TmdbCore"]`.
    - A `PBXNativeTarget` `TmdbNative` (`com.apple.product-type.application`) with a
      `PBXFileSystemSynchronizedRootGroup` `TmdbNative` and `packageProductDependencies = [TmdbCore]`. Its build
      phases are Sources, Frameworks (the `TmdbCore` product) and Resources.
    - A `PBXNativeTarget` `TmdbNativeTests` (`com.apple.product-type.bundle.unit-test`) with a synchronized group
      `TmdbNativeTests`, a target dependency on `TmdbNative`, and `TEST_HOST =
      "$(BUILT_PRODUCTS_DIR)/TmdbNative.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/TmdbNative"`, `BUNDLE_LOADER =
      "$(TEST_HOST)"`.
    - Debug and Release configurations with the settings in Decisions. The `Info.plist` is excluded from the
      synchronized group's Copy Bundle Resources (`PBXFileSystemSynchronizedBuildFileExceptionSet`).
  - **Scheme**: build `TmdbNative`; the test action runs `TmdbNativeTests`.
  - **`TmdbNativeApp.swift`**: `@main struct TmdbNativeApp: App` with `init() { SharedCore.shared.start() }` and
    `body: WindowGroup { PlaceholderHomeView() }`.
  - **`PlaceholderHomeView`**: `ZStack { Theme.night.ignoresSafeArea(); Text("Home").foregroundStyle(.white) }`.
  - **`Theme`**: `enum Theme { static let night = Color(red: 0x15 / 255, green: 0x15 / 255, blue: 0x1D / 255) }`
    (`purple` and `deepPurple` are left out until they are used).
  - **Info.plist and assets**: as in Decisions; `LaunchBackground` is sRGB `#15151D`.
- Unit tests: none in this step (step 2).
- Compile: `./gradlew publishSharedCore` (if `build/swift-package/TmdbCore` is missing), then
  `xcodebuild -project native-ios/TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 2: Unit tests against the published API  (AC-4, AC-7) ✅
- Files: `native-ios/TmdbNativeTests/{StubSettingsRepository.swift, SharedCoreApiTests.swift}`
- What:
  - `final class StubSettingsRepository: SettingsRepository`:
    - `init(onboardingCompleted: Bool)`.
    - `observeOnboardingCompleted() -> any Kotlinx_coroutines_coreFlow` returns `SingleValueFlow(KotlinBoolean(bool:
      onboardingCompleted))`.
    - `completeOnboarding(completionHandler:)` calls back with `AppResultSuccess(value: KotlinUnit())`.
  - `final class SingleValueFlow: Kotlinx_coroutines_coreFlow`:
    `collect(collector:completionHandler:)` → `collector.emit(value: value) { completionHandler($0) }`.
  - `SharedCoreApiTests: XCTestCase`:
    - `testReadsOnboardingCompletedThroughPublishedApi() async throws`: the stub with `true`; collect the first value
      with a `FirstValueCollector` (`Kotlinx_coroutines_coreFlowCollector` that stores the value); expect
      `(value as? KotlinBoolean)?.boolValue == true`.
    - `testReadsNotCompletedThroughPublishedApi() async throws`: the same with `false`.
    - `testSharedCoreProvidesSettingsRepository()`: after `SharedCore.shared.start()` (already called by the host
      app, idempotent), `SharedCore.shared.settingsRepository` is a `SettingsRepository` (AC-4, "started without
      errors").
  - `FirstValueCollector` is a `private final class` in the test file.
- Compile: `xcodebuild -project native-ios/TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 15' build-for-testing`

### Step 3: README  (EC-1, EC-2) ✅
- Files: `native-ios/README.md`
- What:
  - Purpose: a standalone SwiftUI app that uses the shared core like an outside SDK, through the local Swift Package
    `TmdbCore`.
  - Prerequisites: macOS, Xcode 16 or later, JDK 21 (to publish).
  - Workflow:
    1. `./gradlew publishSharedCore` at the repo root.
    2. Open `native-ios/TmdbNative.xcodeproj`, or run `xcodebuild … build`.
    3. After changing the shared core, publish again and rebuild.
  - Tests: `xcodebuild … test` on a simulator.
  - Troubleshooting:
    - Missing package `../build/swift-package/TmdbCore` means the core was not published (EC-1).
    - Old behaviour after a core change means it was not republished (EC-2). If Xcode keeps a stale copy, use
      File → Packages → Reset Package Caches.
- Unit tests: none.
- Compile: none (docs).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-2 builds on its own from the local package | 1 | `xcodebuild … build`; review: `project.pbxproj` references only `../build/swift-package/TmdbCore`, no Gradle task or Kotlin path (subtask "How to test" 1) |
| AC-3 placeholder Home, dark, edge-to-edge, light status bar | 1 | Manual: simulator launch and screenshot (subtask "How to test" 2) |
| AC-4 core started; API readable with a Swift double | 1, 2 | `SharedCoreApiTests.testReadsOnboardingCompletedThroughPublishedApi`, `testReadsNotCompletedThroughPublishedApi`, `testSharedCoreProvidesSettingsRepository`; manual launch with no crash (subtask "How to test" 2, 4) |
| AC-5 installs next to the cmp app | 1 | Manual: install both on one simulator (`com.banhmilabs.tmdb.cmp`, `com.banhmilabs.tmdb.nativeapp`) (subtask "How to test" 3) |
| AC-7 local tests pass | 2 | `xcodebuild … test` on an iPhone simulator (subtask "How to test" 4) |
| EC-1 build before publishing fails clearly | 1, 3 | Manual: move `build/swift-package` aside, build, see the missing-package error; README names the command (subtask "How to test" 5) |
| EC-2 stale core until republished | 3 | Manual (subtask "How to test" 6) |

## Out of scope
- Architecture docs, project guide and rules → 03-architecture-with-native-ios.
- Onboarding, localization and Swift-friendly flow reading for the app → onboarding 07.
- UI tests (XCUITest), CI, signing for devices or distribution, an app icon.

## Risks
- **Hand-written `project.pbxproj`**: a malformed project fails to open. Mitigation: build with `xcodebuild` right
  after step 1. If the synchronized-group format gives trouble, fall back to explicit file references
  (`objectVersion = 56`, as in cmp) and note it as a deviation.
- **Test target and the static Kotlin runtime**: the hosted test target must `import TmdbCore` without linking the
  static framework a second time. Mitigation: the package product goes on the app only. If the test target cannot
  find the module, also add the product to the test target. If that then duplicates the Kotlin runtime
  (duplicate-class warnings or crashes), switch the tests to read through the host app's symbols and note it as a
  deviation.
- **Swift implementing Kotlin's `Flow` and `FlowCollector`**: Kotlin/Native allows Swift classes to adopt
  exported protocols, but Swift 6 strict concurrency may flag the non-`Sendable` Kotlin types. Mitigation: mark the
  test helpers `@unchecked Sendable` or the test class `@MainActor`, the smallest change that compiles. If adopting
  `Kotlinx_coroutines_coreFlow` from Swift is rejected at runtime, stop and ask (large deviation).
- **SKIE flow bridging for the app**: `observeOnboardingCompleted()` is not an `AsyncSequence` in Swift. This is
  not needed here, but it must be solved in onboarding 07's blueprint.

## Deviations
- **AC-4 test uses the real shared core, not a Swift test double** (from user, large deviation). Swift can adopt
  `Kotlinx_coroutines_coreFlow`, but collecting it crashes: "Flow invariant is violated … emission happened in
  EmptyCoroutineContext". Kotlin rejects emissions from a Swift callback.
  - `SharedCoreApiTests` runs in the app's test host. It calls `SharedCore.shared.start()`, `completeOnboarding()`
    (`async throws`), then reads `observeOnboardingCompleted()` with `for await` and expects `true`.
  - `StubSettingsRepository` is removed.
  - task.md and subtask 02 AC-4 are updated.
- **SKIE bridges the flow after all**. Swift sees `observeOnboardingCompleted()` as `SkieSwiftFlow<KotlinBoolean>`,
  an `AsyncSequence`; the Objective-C header only shows the raw type. Suspend members are renamed with a `__`
  prefix plus an `async` wrapper. Onboarding 07 can read the flow with `for await` directly.
- `"EXCLUDED_ARCHS[sdk=iphonesimulator*]" = x86_64` on both targets, as in the cmp project, because the framework
  has no `x86_64` simulator slice.
- The test destination is `iPhone 17` (iOS 26.4); the installed runtimes have no `iPhone 15` simulator.

## Results
- `xcodebuild … build` (generic simulator): succeeded. `xcodebuild … test` on iPhone 17: 2/2 passed.
- Launched on the iPhone 17 simulator: the dark placeholder Home, edge-to-edge, with a light status bar, and no
  crash. `com.banhmilabs.tmdb.cmp` and `com.banhmilabs.tmdb.nativeapp` are installed side by side.
- EC-1: with `build/swift-package` moved aside, `xcodebuild` fails with "the package at
  '…/build/swift-package/TmdbCore' cannot be accessed".
- Not done: EC-2 (republish after a core change).
