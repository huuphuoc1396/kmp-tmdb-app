# Native iOS app

Status: Confirmed
Source: chat (`/spark` request, 2026-11-14)

## Goal
The native iOS app should be a fully independent project. It uses the shared core the way an outside iOS team
would use an SDK: through a published XCFramework, delivered as a local Swift Package, not as part of the Kotlin
Multiplatform build. This mirrors the standalone native Android app ([native-android-app](../native-android-app/task.md)).
It keeps the iOS app apart from the cmp app and from the shared core's build, and it is the base for the onboarding
feature's native iOS subtask (07). Success: the native iOS app builds and runs from its own folder using only the
published framework.

## Scope
### In scope
- The KMP build publishes one XCFramework of the shared core:
  - Release build, with the `iosArm64` and `iosSimulatorArm64` slices.
  - Packaged as a local Swift Package (the framework plus its package manifest) in a git-ignored folder in the
    repo.
  - It exports the shared kernel and the `service/*/domain` APIs, with Swift-friendly interop: `Flow` as an async
    sequence, `suspend` as `async`, sealed types as enums.
  - It has one entry point: a function that starts the shared core, plus accessors that return the domain's
    repositories. Swift never sees the dependency injection framework.
- A standalone iOS project in `native-ios/` at the repo root:
  - Its own committed Xcode project, in SwiftUI, with a minimum of iOS 16.
  - It uses only the published Swift Package and shares nothing else with the KMP build.
- The app:
  - Launches with the app's dark theme, edge-to-edge, with light status bar content, and shows a placeholder Home.
  - Starts the shared core at launch and can read data through the shared core's domain API.
  - Bundle identifier `com.banhmilabs.tmdb.nativeapp`, display name "TMDB Native", so it installs next to the cmp
    iOS app.
- A unit test reads the onboarding completed state through the published API, using the real shared core in the
  app's test host.
- A short README in `native-ios/` describing the publish-then-build workflow.
- The architecture docs, the project guide and the rules describe `native-ios/` and the iOS publishing.

### Out of scope
- The onboarding screen in the native iOS app: onboarding subtask 07, after this feature.
- The real Home screen.
- Building or testing `native-ios/` on CI: CI stays JVM-only, and apps build locally.
- Publishing to a remote repository, code signing for distribution, the App Store.
- The Intel simulator (`iosX64`).
- Exporting the Kotlin service fakes to Swift.
- Static analysis for Swift (for example SwiftLint).

## User flow
For a developer (macOS + Xcode):
1. In the KMP build, run the iOS publish command: the XCFramework and its Swift Package land in the in-repo folder.
2. Open `native-ios/` in Xcode (or use `xcodebuild`), then build and run the app on a simulator. It resolves the
   shared core from that package.
3. After changing the shared core, publish again, then rebuild the app to pick up the change.

For an app user:
1. Launch the native iOS app: it opens on the placeholder Home in the dark theme.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| Home (placeholder) | App launched | The dark background with a "Home" label; no actions yet |

## Acceptance criteria
- **AC-1** Shared core is published for iOS
  - Given the KMP build on macOS with Xcode
  - When the developer runs the iOS publish command
  - Then a release XCFramework with the `iosArm64` and `iosSimulatorArm64` slices is in the in-repo folder,
    with a Swift Package that provides it
  - And it exposes the shared core's domain API and one entry point to start the core and get its repositories
  - And it contains no test-only code (service fakes, test utilities) and no app code

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
  - And a unit test in the app reads the onboarding completed state through the published API, using the real
    shared core in the app's test host (no published fakes)

- **AC-5** Installs next to the cmp app
  - Given the cmp iOS app is installed
  - When the native iOS app is installed
  - Then both are installed and run side by side

- **AC-6** KMP build is free of native iOS app code
  - Given the KMP build
  - Then it contains no native iOS app code
  - And the CI checks (`detekt jvmTest koverVerify :architecture-test:test`) pass without building `native-ios/`

- **AC-7** Local tests
  - Given `native-ios/`
  - When the developer runs its unit tests on a simulator
  - Then they run locally and pass

## Edge cases
- **EC-1** The app is built before the shared core was published for iOS -> the build fails because the package
  can't be found. The README names the publish command to run first.
- **EC-2** The shared core changed but was not published again -> the app keeps using the previously published
  framework until the developer publishes again.
- **EC-3** The shared core is published again -> the new framework replaces the old one, and the next app build
  uses it.

## Constraints
- iOS 16 minimum, Swift and SwiftUI, the same iOS level as the KMP build.
- macOS and Xcode are needed to publish the framework and to build the app.
- Works fully offline; no TMDB token needed.
- `native-ios/` never depends on the cmp app, on `native-android/` or on any app module; it uses only the published
  shared core.
- CI is unchanged (JVM checks only).

## Open questions
- None.

## Change log
- 2026-11-14: Initial version.
- 2026-11-16: AC-4 test uses the real shared core in the test host; Kotlin rejects a `Flow` implemented in Swift
  (user decision during 02).

## Subtasks
1. [01 - Publish shared core for iOS](subtasks/01-publish-shared-core-ios.md)
2. [02 - Standalone native iOS app](subtasks/02-standalone-native-ios-app.md)
3. [03 - Architecture with native-ios](subtasks/03-architecture-with-native-ios.md)
