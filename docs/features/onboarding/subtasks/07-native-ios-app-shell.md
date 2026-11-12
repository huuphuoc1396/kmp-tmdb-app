# 07 - App shell (native iOS)

Feature: [task.md](../task.md)
Depends on: 02-onboarding-completed-state
Status: Todo

## Goal
The native iOS app (SwiftUI) exists in `apps/native` and launches. It links the shared core through one umbrella
framework, starts it, uses the app's dark theme, and shows a placeholder Home screen. Nothing from onboarding is
visible yet; 09 adds the onboarding screen on top of this shell.

## Scope
### In scope
- `apps/native/shared`, the iOS umbrella framework (`docs/architecture.md` 11):
  - It exports `core:common`, `core:model` and `service/*/domain`, plus `service/*/fake` in debug builds.
  - It uses SKIE, so `Flow` becomes `AsyncSequence` and `suspend` becomes `async`.
  - A `KoinHelper` gives Swift access to repositories.
  - It has a place in the module rules.
- `apps/native/iosApp`, the Xcode project:
  - SwiftUI app that starts the shared core at launch.
  - Dark theme with the design's purple accents, written in Swift.
  - Edge-to-edge with light status bar content.
  - A `NavigationStack` with a placeholder Home as the root.
- Swift tests target with XCTest, ready for the first Swift ViewModel (architecture 8).
- App: `apps/native` (on `main`).
### Out of scope (handled later)
- The Android app → 06-native-android-app-shell
- The onboarding screen, start-screen choice and "Acessar" → 09-native-ios-onboarding
- The real Home screen → Home feature

## Acceptance criteria
None from task.md directly. This subtask is the prerequisite for AC-1–AC-8 on the native iOS app, and it provides
the placeholder Home that AC-2 opens until the Home feature exists.

## Edge cases
None from task.md; EC-1–EC-6 are handled in 09.

## Layers touched
- Build logic (`shared` framework, SKIE, module rules)
- App shell (SwiftUI app, Koin start through `KoinHelper`)
- Theme (dark, purple accent)
- Navigation (`NavigationStack`, placeholder Home)

## How to test
1. Build and run the native iOS app on the simulator: it launches without crashing and shows the placeholder Home
   in the dark theme.
2. The app links exactly one Kotlin framework (`shared`) and can read a repository through `KoinHelper`, for
   example the onboarding completed state, from Swift.
3. Run the JVM checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`). They pass, including the
   module rules for `apps/native/shared`.
4. Run `xcodebuild` for the iOS simulator; the Swift test target builds and runs.

## Change log
- 2026-11-12: Initial version (prerequisite split out of the native iOS onboarding).
