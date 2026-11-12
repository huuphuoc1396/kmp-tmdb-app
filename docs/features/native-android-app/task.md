# Native Android app

Status: Confirmed
Source: chat (`/spark` request, 2026-11-12)

## Goal
The native Android app should be a fully independent project. It uses the shared core the way an outside Android
team would use an SDK: through published Android libraries (AAR), not as modules of the Kotlin Multiplatform
build. This keeps the native app apart from the cmp app and from the shared core's build. The shared core becomes
a product with a clear published surface. Success: the native Android app builds and runs from its own folder
using only the published libraries, and the KMP build has no native Android modules.

## Scope
### In scope
- The KMP build publishes an Android library with its POM for every production module of `core/` and `service/`,
  to a Maven repository inside the repo (git-ignored), at version `0.1.0-SNAPSHOT`.
- A standalone Android project in `native-android/` at the repo root. It has its own settings, Gradle wrapper,
  version catalog and static-analysis configuration, and shares nothing with the KMP build except the published
  libraries.
- The app:
  - Launches with the app's dark theme, edge-to-edge with light system bar icons, and shows a placeholder Home.
  - Starts the shared core at launch and can read data through the shared core's domain API.
- The KMP build no longer contains native Android app modules (`apps/native`). The architecture docs and module
  rules describe the new setup.
- A short README in `native-android/` describing the publish-then-build workflow.

### Out of scope
- A standalone native iOS app (`native-ios/`, consuming an XCFramework): later, in the same spirit.
- The onboarding screen in the native Android app: done by the onboarding feature, retargeted to this app.
- Building or testing `native-android/` on CI: CI stays JVM-only, and apps build locally.
- Publishing to a remote repository (Maven Central, GitHub Packages).
- Publishing test-only modules (`service/*/fake`, `core:testing`).
- The real Home screen.

## User flow
For a developer:
1. In the KMP build, run the publish command: the shared core's libraries land in the in-repo Maven repository.
2. In `native-android/`, build or install the app: it resolves the shared core from that repository.
3. After changing the shared core, publish again, then rebuild the app to pick up the change.

For an app user:
1. Launch the native Android app: it opens on the placeholder Home in the dark theme.

## UI states
| State | When | What the user sees / can do |
|---|---|---|
| Home (placeholder) | App launched | The dark background with a "Home" label; no actions yet |

## Acceptance criteria
- **AC-1** Shared core is published
  - Given the KMP build
  - When the developer runs the publish command
  - Then every production module of `core/` and `service/` is in the in-repo Maven repository as an Android
    library with a POM listing its dependencies, at version `0.1.0-SNAPSHOT`
  - And test-only modules (`service/*/fake`, `core:testing`) are not published

- **AC-2** Native Android app builds on its own
  - Given the shared core is published
  - When the developer builds the app from `native-android/`
  - Then it builds using only its own Gradle files and the in-repo Maven repository, with no project dependency,
    included build or source reference to the KMP build

- **AC-3** App launches on the placeholder Home
  - Given the app is installed
  - When the user launches it
  - Then the placeholder Home shows in the dark theme, edge-to-edge, with light system bar icons

- **AC-4** App uses the shared core
  - Given the app is launched
  - Then the shared core is started without errors
  - And a unit test in the app reads the onboarding completed state through the shared core's published domain
    API (MockK for collaborators, no published fakes)

- **AC-5** Installs next to the cmp app
  - Given the cmp app is installed
  - When the native Android app is installed
  - Then both are installed and run side by side

- **AC-6** KMP build is free of native Android modules
  - Given the KMP build
  - Then it contains no native Android app modules
  - And the CI checks (`detekt jvmTest koverVerify :architecture-test:test`) pass without building
    `native-android/`

- **AC-7** Local quality checks
  - Given `native-android/`
  - When the developer runs its static analysis and unit tests
  - Then both run locally and pass

## Edge cases
- **EC-1** The app is built before the shared core was published -> the build fails because the dependencies
  can't be found. The README names the publish command to run first.
- **EC-2** The shared core changed but was not published again -> the app keeps using the previously published
  libraries until the developer publishes again.
- **EC-3** The shared core is published again at the same `0.1.0-SNAPSHOT` version -> the new libraries replace
  the old ones, and the next app build uses them.

## Constraints
- Android minSdk 24, compileSdk and targetSdk 37, JDK 21 (same levels as the KMP build).
- UI with Jetpack Compose.
- Works fully offline; no TMDB token needed.
- `native-android/` never depends on the cmp app or on any app module; it uses only the published shared core.
- CI is unchanged (JVM checks only).

## Open questions
- None.

## Change log
- 2026-11-12: Initial version.

## Subtasks
1. [01 - Publish shared core](subtasks/01-publish-shared-core.md)
2. [02 - Standalone native Android app](subtasks/02-standalone-native-android-app.md)
3. [03 - Architecture without apps/native](subtasks/03-architecture-without-apps-native.md)
