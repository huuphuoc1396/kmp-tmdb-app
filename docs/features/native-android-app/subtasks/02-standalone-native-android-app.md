# 02 - Standalone native Android app

Feature: [task.md](../task.md)
Depends on: 01-publish-shared-core
Status: Done

## Goal
`native-android/` is a standalone Android project that builds against the published shared core only. The app
launches on a placeholder Home in the dark theme, starts the shared core, and installs next to the cmp app.

## Scope
### In scope
- The standalone project in `native-android/`: its own settings, Gradle wrapper, version catalog and
  static-analysis configuration. It resolves the shared core from the in-repo Maven repository.
- The app, built with Jetpack Compose:
  - Starts the shared core at launch.
  - Dark theme, edge-to-edge with light system bar icons.
  - Navigation with a placeholder Home.
  - A different application ID and label from the cmp app.
- A unit test that reads the onboarding completed state through the shared core's published domain API, using
  MockK for collaborators.
- A README describing the publish-then-build workflow and the command to run first.
### Out of scope (handled later)
- Docs and module rules of the KMP build -> 03-architecture-without-apps-native
- The onboarding screen (onboarding feature, retargeted to this app), the real Home, and CI (not in this feature)

## Acceptance criteria
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
- **AC-7** Local quality checks
  - Given `native-android/`
  - When the developer runs its static analysis and unit tests
  - Then both run locally and pass

## Edge cases
- **EC-1** The app is built before the shared core was published -> the build fails because the dependencies
  can't be found. The README names the publish command to run first.
- **EC-2** The shared core changed but was not published again -> the app keeps using the previously published
  libraries until the developer publishes again.

## Layers touched
- Standalone build (settings, wrapper, version catalog, static analysis)
- App shell (application start-up, activity, theme, navigation)
- Tests (unit test against the published domain API)
- Docs (README of the project)

## How to test
1. Publish the shared core (01), then build the app from `native-android/`: it builds. Check that its build files
   reference only the in-repo Maven repository for the shared core, with no included build or project path into
   the KMP build (AC-2).
2. Install and launch it on an emulator: the placeholder Home in the dark theme, edge-to-edge, light system bar
   icons, no crash (AC-3, AC-4).
3. Install the cmp app too: both are installed and run (AC-5).
4. Run static analysis and the unit tests in `native-android/`: they pass, including the test that reads the
   onboarding completed state (AC-4, AC-7).
5. With a clean in-repo Maven repository, build `native-android/`: it fails to resolve the shared core, and the
   README names the publish command (EC-1).
6. Change the shared core without publishing and rebuild the app: it still uses the old libraries. Publish and
   rebuild: the change is picked up (EC-2).

## Change log
- 2026-11-12: Initial version.
