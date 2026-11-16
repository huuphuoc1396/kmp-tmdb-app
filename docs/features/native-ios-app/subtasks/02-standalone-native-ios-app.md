# 02 - Standalone native iOS app

Feature: [task.md](../task.md)
Depends on: 01-publish-shared-core-ios
Status: Todo

## Goal
`native-ios/` is a standalone iOS project that builds against the published Swift Package only. The app launches
on a placeholder Home in the dark theme, starts the shared core, and installs next to the cmp iOS app.

## Scope
### In scope
- The standalone project in `native-ios/`: a committed Xcode project, SwiftUI, iOS 16 minimum, depending only on
  the local Swift Package published in 01.
- The app shell: dark theme, edge-to-edge with light status bar content, a placeholder Home, and the shared core
  started at launch through the framework's entry point.
- Bundle identifier `com.banhmilabs.tmdb.nativeapp`, display name "TMDB Native".
- A unit test target that reads the onboarding completed state through the published API, with a test double
  written in Swift.
- A short README in `native-ios/` with the publish-then-build workflow.
### Out of scope (handled later)
- Architecture docs, project guide and rules -> 03-architecture-with-native-ios
- The onboarding screen -> onboarding subtask 07

## Acceptance criteria
- **AC-2** Native iOS app builds on its own
  - Given the shared core is published for iOS
  - When the developer builds the app from `native-ios/`
  - Then it builds using only its own Xcode project and the local Swift Package, with no reference to the Gradle
    build or to Kotlin sources
- **AC-3** App launches on the placeholder Home
  - Given the app is installed on a simulator
  - When the user launches it
  - Then the placeholder Home shows in the dark theme, edge-to-edge, with light status bar content
- **AC-4** App uses the shared core
  - Given the app is launched
  - Then the shared core is started without errors
  - And a unit test in the app reads the onboarding completed state through the published API, using a test
    double written in Swift (no published fakes)
- **AC-5** Installs next to the cmp app
  - Given the cmp iOS app is installed
  - When the native iOS app is installed
  - Then both are installed and run side by side
- **AC-7** Local tests
  - Given `native-ios/`
  - When the developer runs its unit tests on a simulator
  - Then they run locally and pass

## Edge cases
- **EC-1** The app is built before the shared core was published for iOS -> the build fails because the package
  can't be found. The README names the publish command to run first.
- **EC-2** The shared core changed but was not published again -> the app keeps using the previously published
  framework until the developer publishes again.

## Layers touched
- Xcode project and package dependency
- App shell (SwiftUI)
- Unit tests (XCTest)
- README

## How to test
1. Publish the shared core for iOS, then build `native-ios/` with Xcode or `xcodebuild` for the simulator: it
   builds. Its project files reference only the local package, never the Gradle build or Kotlin sources (AC-2).
2. Launch on a simulator: the placeholder Home in the dark theme, edge-to-edge, light status bar (AC-3), and no
   error at start-up (AC-4).
3. Install the cmp iOS app and the native app on the same simulator: both appear and run (AC-5).
4. Run the unit tests with `xcodebuild test` on a simulator: they pass (AC-4, AC-7).
5. Move the published package folder aside and build: the build fails because the package is missing, and the
   README names the publish command (EC-1).
6. Change a shared module without publishing, rebuild the app: it still uses the old framework; publish, rebuild:
   the change is picked up (EC-2).

## Change log
- 2026-11-14: Initial version.
