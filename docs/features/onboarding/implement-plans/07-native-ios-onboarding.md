# Plan: Onboarding on iOS (native)

Requirement: [07 - Onboarding on iOS (native)](../subtasks/07-native-ios-onboarding.md)
Status: Done

## Summary
The onboarding of `native-android/` (plan 06) is ported to the standalone `native-ios/` app in SwiftUI. It keeps
the same MVI shape, layout, copy, logo and start-screen logic.

- **Start screen:** a root `AppViewModel` reads the onboarding completed state once. Until then the root shows the
  dark launch colour, so no wrong screen flashes (AC-3).
- **Button:** "Get started" / "Acessar" records completion and then swaps the root to the placeholder Home. There is
  no navigation stack, so there is nothing to go back to (AC-2, AC-4, EC-1, EC-3).
- **Testing:** the ViewModels depend on a small Swift protocol, `OnboardingSettings`. An adapter implements it over
  `SharedCore.shared.settingsRepository`. ViewModel tests use a Swift test double of the protocol, and the adapter
  is tested against the real shared core in the test host (from user).

## Decisions
- **ViewModels depend on the Swift protocol `OnboardingSettings`** (from user).
  - It has two methods: `isOnboardingCompleted() async -> Bool` and `completeOnboarding() async throws`.
  - `SharedCoreOnboardingSettings` implements it with the published `SettingsRepository`.
  - Why: Swift can't implement a Kotlin `Flow`, and the real core keeps its state between test runs with no reset
    API. A Swift protocol lets the tests cover a fresh install, a completed install, a failed save and a double
    tap.
  - Rejected: ViewModels on `SettingsRepository` directly with real-core tests only, which can't test a fresh
    install or a failed save. Rejected: no ViewModel tests.
  - The architecture doc's native iOS testing row and section 11 are updated to match.
- **The test double is `OnboardingSettingsStub`, in `TmdbNativeTests/`.** CLAUDE.md's rule against ad-hoc `Fake*`
  classes is about Kotlin test code. There is no MockK on iOS, and the service fakes are not exported to Swift
  (native-ios-app out of scope).
- **ViewModels follow the CLAUDE.md MVI shape, in Swift.** Each is a `@MainActor final class` that is an
  `ObservableObject` (native-ios-app 03).
  - State: `@Published private(set) var state: OnboardingUiState` (a struct).
  - Intents: `OnboardingIntent` (an enum), handled by `onIntent(_:)`.
  - Effects: `OnboardingEffect` (an enum), exposed as `effects: AsyncStream<OnboardingEffect>` (a buffered
    stream).
  - The stateful `OnboardingView` owns the ViewModel (`@StateObject`) and collects effects in `.task`. It draws the
    stateless `OnboardingContent(state:onIntent:)`.
- **Navigation is a root switch, not a `NavigationStack`.** `AppRootView` switches on
  `AppViewModel.startDestination` (`.loading`, `.onboarding` or `.home`), and `onboardingFinished()` sets `.home`.
  Onboarding is never in a stack, so there is no swipe back to it (AC-4). iOS has no system Back: on onboarding,
  "Back" means leaving the app with the Home gesture.
- **Same design decisions as cmp and Android** (plans 04–06, design/onboarding.md), not re-asked:
  - The official TMDB logo keeps its own colours, at 15pt high.
  - The headline is one Light-weight string marked as a header.
  - The artwork is the `#303243` → `#3D458F` gradient placeholder (stop at 56.527%), under the scrim gradient
    (transparent to 30.89%, `rgba(21,21,29,0.85)` from 62.685%). Both draw full-bleed, ignoring safe areas.
  - The content block is bottom-anchored, max width 315pt, with paddings 29 / 31 / 31 inside the safe area, and
    scrolls when it doesn't fit.
  - The button is a gradient pill (`#8000FF` → `#540BA1`, top-leading to bottom-trailing) with min height 48pt and a
    pressed highlight.
  - The ViewModel navigates after `completeOnboarding()` returns or throws. `isNavigating` ignores repeated taps.
- **The gradients are SwiftUI `LinearGradient`s, not assets.** iOS asset catalogs can't hold vector gradients, and
  the values stay private in the view (as on Android). The button colours go in `Theme`
  (`Theme.primary` = `#8000FF`, `Theme.primaryContainer` = `#540BA1`), the way Android reads them from its theme.
- **Fonts: the system font with Dynamic Type.** Android and cmp don't ship Inter either. The sizes use
  `@ScaledMetric`:
  - headline 32 Light, relative to `.largeTitle`, line spacing 5.3;
  - body 15 Regular, relative to `.subheadline`, line spacing 3.75;
  - button 16 SemiBold, relative to `.body`.
  This way text grows with the largest accessibility sizes (AC-7, EC-5).
- **Strings: a String Catalog `Localizable.xcstrings`** with the keys `onboarding.headline`, `onboarding.body` and
  `onboarding.getStarted`, in `en` (the development language) and `pt-BR`, with the copy of `native-android`. Add
  `pt-BR` to the project's `knownRegions`. Any other language falls back to English (AC-5, EC-6).
- **Logo: `Assets.xcassets/TmdbLogo.imageset`** holds the SVG copied from `design/assets/tmdb_logo_long.svg`, with
  "Preserve Vector Data". It is shown with `Image(decorative:)` so VoiceOver skips it.
- **Light status bar content:** it already comes from `UIUserInterfaceStyle Dark` (native-ios-app 02). No change.
- **Package layout by screen:**
  - The app root keeps `TmdbNativeApp`, `AppRootView`, `AppViewModel`, `StartDestination`, `PlaceholderHomeView` and
    `Theme`.
  - `Onboarding/` holds `OnboardingSettings` (the protocol and its `SharedCoreOnboardingSettings` adapter),
    `OnboardingContract`, `OnboardingViewModel`, `OnboardingView` (with `OnboardingContent`) and
    `Component/GetStartedButton`.
  - The folders are file-system synchronized, so new files need no project edits.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Onboarding state | `SettingsRepository.observeOnboardingCompleted()` (`SkieSwiftFlow<KotlinBoolean>`), `completeOnboarding() async throws -> any AppResult` | published `TmdbCore` (`SharedCore.shared.settingsRepository`) |
| Shared core start-up | `SharedCore.shared.start()` | `native-ios/TmdbNative/TmdbNativeApp.swift` |
| Reading a SKIE flow once | `for await value in flow { return value.boolValue }` | `native-ios/TmdbNativeTests/SharedCoreApiTests.swift` |
| Contract, ViewModel, layout values, button, start-screen logic, test cases | `OnboardingContract`, `OnboardingViewModel`, `OnboardingScreen`, `GetStartedButton`, `AppViewModel`, `StartDestination`, the two ViewModel tests | `native-android/app/src/{main,test}/kotlin/com/banhmilabs/tmdb/nativeapp/` (ported) |
| Copy en / pt-BR | `strings.xml` | `native-android/app/src/main/res/values{,-pt-rBR}/` (copied into the String Catalog) |
| Logo | `tmdb_logo_long.svg` | `docs/features/onboarding/design/assets/` |
| Dark background, placeholder Home, launch colour | `Theme.night`, `PlaceholderHomeView`, `LaunchBackground` | `native-ios/TmdbNative/` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-ios` app | `TmdbNative/Localizable.xcstrings` (new), `Assets.xcassets/TmdbLogo.imageset/{Contents.json,tmdb_logo.svg}` (new), `Theme.swift`, `TmdbNative.xcodeproj/project.pbxproj` (`knownRegions`) (modify) | Copy, logo, button colours |
| `native-ios` app | `Onboarding/OnboardingSettings.swift`, `OnboardingContract.swift`, `OnboardingViewModel.swift`, `OnboardingView.swift`, `Component/GetStartedButton.swift` (new) | Onboarding screen |
| `native-ios` app | `AppRootView.swift`, `AppViewModel.swift`, `StartDestination.swift` (new), `TmdbNativeApp.swift` (modify) | Start screen and root switch |
| `native-ios` tests | `OnboardingSettingsStub.swift`, `OnboardingViewModelTests.swift`, `AppViewModelTests.swift` (new), `SharedCoreApiTests.swift` (modify) | Tests |
| docs | `docs/architecture.md` (§8 native iOS row, §11 native-ios bullet) (modify) | Record the Swift protocol and test-double choice |

## Steps
Prerequisite: `./gradlew publishSharedCore` (on macOS) has written `build/swift-package/TmdbCore`; it is present now.
Build command (from `native-ios/`):
`xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.

### Step 1: Resources and theme  (AC-1, AC-5, EC-6) ✅
- Files:
  - `TmdbNative/Localizable.xcstrings` (new).
  - `TmdbNative/Assets.xcassets/TmdbLogo.imageset/Contents.json` and `tmdb_logo.svg` (new).
  - `TmdbNative/Theme.swift` and `TmdbNative.xcodeproj/project.pbxproj` (modify).
- What:
  - The String Catalog has `sourceLanguage` `en` and the 3 keys above, with en and pt-BR values copied from the
    native-android `strings.xml`.
  - The imageset holds the SVG with `"preserves-vector-representation": true`.
  - Add `static let primary` (`#8000FF`) and `static let primaryContainer` (`#540BA1`) to `Theme`.
  - Add `"pt-BR"` to `knownRegions`.
- Unit tests: none.
- Compile: the build command.

### Step 2: Onboarding screen  (AC-1, AC-2, AC-5, AC-6, AC-7, AC-8, EC-1, EC-3, EC-5) ✅
- Files: `TmdbNative/Onboarding/OnboardingSettings.swift`, `OnboardingContract.swift`, `OnboardingViewModel.swift`,
  `OnboardingView.swift`, `Component/GetStartedButton.swift` (new).
- What:
  - **`OnboardingSettings.swift`:**
    - `@MainActor protocol OnboardingSettings { func isOnboardingCompleted() async -> Bool; func completeOnboarding() async throws }`.
    - `@MainActor struct SharedCoreOnboardingSettings: OnboardingSettings`, using
      `SharedCore.shared.settingsRepository`.
      - `isOnboardingCompleted` reads the first value of `observeOnboardingCompleted()` with `for await` (`false` if
        the flow ends empty).
      - `completeOnboarding` does `_ = try await repository.completeOnboarding()`. An `AppResult` failure is
        ignored, like Android (EC-1).
  - **`OnboardingContract.swift`:**
    - `struct OnboardingUiState: Equatable { var isNavigating = false }`.
    - `enum OnboardingIntent { case getStartedTapped }`.
    - `enum OnboardingEffect { case navigateToHome }`.
  - **`OnboardingViewModel.swift`:** `@MainActor final class OnboardingViewModel: ObservableObject`.
    - `@Published private(set) var state = OnboardingUiState()`.
    - `let effects: AsyncStream<OnboardingEffect>`, made with `AsyncStream.makeStream()` and a private
      continuation.
    - `init(settings: any OnboardingSettings)`.
    - `onIntent(_:)`: `.getStartedTapped` → `getStarted()`.
    - Pseudo-code for `getStarted()`:
      ```swift
      guard !state.isNavigating else { return }
      state.isNavigating = true
      Task { try? await settings.completeOnboarding(); continuation.yield(.navigateToHome) }
      ```
  - **`OnboardingView.swift`:**
    - `struct OnboardingView: View`.
      - It has `@StateObject private var viewModel` and `let onFinished: () -> Void`.
      - `init(settings: any OnboardingSettings, onFinished:)` uses
        `_viewModel = StateObject(wrappedValue: OnboardingViewModel(settings: settings))`.
      - The body is `OnboardingContent(state: viewModel.state, onIntent: viewModel.onIntent)`, plus
        `.task { for await effect in viewModel.effects { switch effect { case .navigateToHome: onFinished() } } }`.
    - `struct OnboardingContent: View`.
      - The layers, in a `ZStack`:
        1. the artwork `LinearGradient`, with `.ignoresSafeArea()` and `.accessibilityHidden(true)`;
        2. the scrim `LinearGradient`, also with `.ignoresSafeArea()` and `.accessibilityHidden(true)`;
        3. a `GeometryReader` holding a `ScrollView`. Inside it, a
           `VStack(alignment: .leading, spacing: 19)` holds, in order:
           - `Image(decorative: "TmdbLogo")`, resizable, scaled to fit, height 15;
           - the headline `Text("onboarding.headline")`, with `.accessibilityAddTraits(.isHeader)`;
           - the body `Text("onboarding.body")`;
           - `GetStartedButton(enabled: !state.isNavigating) { onIntent(.getStartedTapped) }`.
           Each `Text` uses `.fixedSize(horizontal: false, vertical: true)` and white text.
      - The stack gets `.frame(maxWidth: 315, alignment: .leading)`, then `.padding(.leading, 29)`,
        `.padding(.trailing, 31)` and `.padding(.bottom, 31)`, then
        `.frame(maxWidth: .infinity, minHeight: geometry.size.height, alignment: .bottom)`. The content stays at
        the bottom and scrolls only when it overflows.
      - The fonts use `@ScaledMetric` sizes, as in Decisions.
  - **`Component/GetStartedButton.swift`:**
    - `struct GetStartedButton: View`, with `enabled: Bool` and `action: () -> Void`.
    - The label is `Button(action:)` with `Text("onboarding.getStarted")`: SemiBold scaled 16, white, centered,
      `frame(maxWidth: .infinity, minHeight: 48)`, 12pt padding.
    - It uses a private `ButtonStyle`:
      - the `Theme.primary` → `Theme.primaryContainer` gradient, in a `Capsule`;
      - opacity 0.8 while pressed;
      - no dimming when disabled.
    - It has `.disabled(!enabled)`.
- Unit tests: in Step 4.
- Compile: the build command.

### Step 3: Start screen and root switch  (AC-1, AC-2, AC-3, AC-4, EC-2, EC-4) ✅
- Files: `TmdbNative/StartDestination.swift`, `AppViewModel.swift` and `AppRootView.swift` (new);
  `TmdbNativeApp.swift` (modify).
- What:
  - `enum StartDestination { case loading, onboarding, home }`.
  - `@MainActor final class AppViewModel: ObservableObject`:
    - `@Published private(set) var startDestination: StartDestination = .loading`.
    - `init(settings: any OnboardingSettings)`.
    - `func load() async`: only while `.loading`, it sets `isOnboardingCompleted()` ? `.home` : `.onboarding`.
    - `func onboardingFinished()` sets `.home`.
  - `struct AppRootView: View`:
    - It holds `@StateObject private var viewModel` and `let settings: any OnboardingSettings`.
    - The body switches on the destination:
      - `.loading`: `Theme.night.ignoresSafeArea()`, the launch colour, so no flash;
      - `.onboarding`: `OnboardingView(settings: settings, onFinished: viewModel.onboardingFinished)`;
      - `.home`: `PlaceholderHomeView()`.
    - It has `.task { await viewModel.load() }`.
  - `TmdbNativeApp`:
    - `init()` still calls `SharedCore.shared.start()`.
    - `WindowGroup { AppRootView(settings: SharedCoreOnboardingSettings()) }`.
- Unit tests: in Step 4.
- Compile: the build command.

### Step 4: Tests  (AC-2, AC-3, EC-1, EC-3) ✅
- Files:
  - `TmdbNativeTests/OnboardingSettingsStub.swift`, `OnboardingViewModelTests.swift` and `AppViewModelTests.swift`
    (new).
  - `SharedCoreApiTests.swift` (modify).
- What:
  - `@MainActor final class OnboardingSettingsStub: OnboardingSettings` with `var completed = false`,
    `var completeError: Error?` and `private(set) var completeCalls = 0`. `completeOnboarding` counts the call,
    throws `completeError` if set, and otherwise sets `completed = true`.
  - `@MainActor final class OnboardingViewModelTests: XCTestCase`:
    - `testInitialStateIsNotNavigating`;
    - `testTapCompletesOnboardingAndNavigatesToHome`: await the first `effects` value, then expect
      `.navigateToHome`, `completeCalls == 1` and `state.isNavigating`;
    - `testRepeatedTapsCompleteOnce` (EC-3): call `onIntent` twice, await the first effect, then expect
      `completeCalls == 1`;
    - `testFailedSaveStillNavigatesToHome` (EC-1): with `completeError` set, expect `.navigateToHome`.
  - `@MainActor final class AppViewModelTests: XCTestCase`:
    - `testStartsLoading`;
    - `testNotCompletedStartsOnOnboarding`;
    - `testCompletedStartsOnHome` (AC-3);
    - `testOnboardingFinishedShowsHome` (AC-2).
  - `SharedCoreApiTests`: add `testAdapterReadsCompletionThroughSharedCore`. It runs
    `SharedCoreOnboardingSettings().completeOnboarding()`, then expects `isOnboardingCompleted() == true`, against
    the real core.
- Run (from `native-ios/`):
  `xcodebuild -project TmdbNative.xcodeproj -scheme TmdbNative -destination 'platform=iOS Simulator,name=iPhone 17' test`.

### Step 5: Architecture doc  (traceability for the testing choice) ✅
- Files: `docs/architecture.md` (modify).
- What:
  - **§8, the native iOS row:** "XCTest: ViewModels against small Swift protocols with Swift test doubles; the
    adapters over `SharedCore` in the app's test host against the real shared core (Swift can't implement a Kotlin
    `Flow`); UI checked manually on the simulator".
  - **§11, the native-ios ViewModel bullet:** add "ViewModels depend on small Swift protocols. Adapters implement
    them over `SharedCore`'s repositories."
- Compile: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 Onboarding on first launch | 1, 2, 3 | `AppViewModelTests.testNotCompletedStartsOnOnboarding`; manual 1 |
| AC-2 Button enters the app | 2, 3 | `OnboardingViewModelTests.testTapCompletesOnboardingAndNavigatesToHome`, `AppViewModelTests.testOnboardingFinishedShowsHome`; manual 2 |
| AC-3 Skipped after completion, no flash | 3 | `AppViewModelTests.testCompletedStartsOnHome`, `testStartsLoading`, `SharedCoreApiTests.testAdapterReadsCompletionThroughSharedCore`; manual 4 |
| AC-4 Not in the back stack | 3 | manual 3 (root switch, no swipe back) |
| AC-5 Localized copy | 1 | manual 6 |
| AC-6 Works offline | 2 | manual 7 (no network code in onboarding) |
| AC-7 Accessible | 2 | manual 9, 10 (VoiceOver via Accessibility Inspector, largest Dynamic Type size) |
| AC-8 Orientation and sizes | 2 | manual 11 (iPhone and iPad, portrait and landscape) |
| EC-1 Save fails | 2 | `OnboardingViewModelTests.testFailedSaveStillNavigatesToHome` |
| EC-2 Killed before tap | 3 | manual 5 |
| EC-3 Repeated taps | 2 | `OnboardingViewModelTests.testRepeatedTapsCompleteOnce`; manual 2 |
| EC-4 Rotation / process death | 2, 3 | manual 12 |
| EC-5 Long copy, large font | 2 | manual 10 |
| EC-6 Unsupported language | 1 | manual 6 (fr) |

## Out of scope
- Android: done in 06.
- The real Home screen: the Home feature.
- UI tests (XCUITest) and snapshot tests: the UI is checked manually (architecture §8).
- Licensed artwork: the gradient placeholder stays until it is supplied.

## Risks
- **The test host launches the app, so `AppRootView` also starts and reads the real core during tests.** The
  ViewModel tests use the stub, so they aren't affected. `SharedCoreApiTests` only writes "completed", which is
  what the tests expect.
- **The real core keeps "completed" in the simulator's app sandbox after the tests.** A manual first-launch check
  (manual 1) needs the app deleted from the simulator first (`xcrun simctl uninstall booted
  com.banhmilabs.tmdb.nativeapp`).
- **Swift 6 strict concurrency with non-`Sendable` Kotlin types.** The protocol, the adapter and the ViewModels
  are all `@MainActor`, so Kotlin objects stay on one actor. If SKIE's async wrappers still warn, isolate the adapter
  call and note it as a deviation.
- **`ScrollView` bounces even when the content fits.** `scrollBounceBehavior` needs iOS 16.4, while the minimum is
  16.0. This is accepted, because the content still sits at the bottom.

## Deviations
- `SharedCoreOnboardingSettings` reads `SharedCore.shared.settingsRepository` inside each method instead of keeping
  it in a stored property. With a stored property, Swift 6 reports "sending value of non-`Sendable` type
  `any SettingsRepository` risks causing data races" when calling the SKIE `async` wrapper from the main actor.
- The test files import the app with `@testable import TmdbNative`, which the plan did not spell out.

## Results
- Build: `xcodebuild … -destination 'generic/platform=iOS Simulator' … build` passed after each step.
- Tests: `xcodebuild … -destination 'platform=iOS Simulator,name=iPhone 17' test` — 11 tests, 0 failures
  (`OnboardingViewModelTests` 4, `AppViewModelTests` 4, `SharedCoreApiTests` 3).

## Manual checks (iPhone 17 simulator, iOS 26.4)
- Done:
  - A fresh install shows onboarding with the logo, headline, body and button (AC-1).
  - A double tap opens Home once (AC-2, EC-3).
  - An edge swipe on Home does not bring onboarding back (AC-4).
  - Kill and relaunch opens Home (AC-3).
  - pt-BR shows the Portuguese copy and "Acessar", and fr shows English (AC-5, EC-6).
  - Landscape at the largest accessibility text size: the text wraps, the content scrolls, and the button stays
    reachable (AC-7, AC-8, EC-5).
- Not done:
  - VoiceOver (AC-7).
  - Airplane mode (AC-6). Onboarding has no network code.
  - Killing the app before the tap (EC-2).
  - Process death on onboarding (EC-4).
  - An iPad.
  - A forced save failure on the device (EC-1, covered by a unit test).
