# 03 - App shell (cmp)

Feature: [task.md](../task.md)
Depends on: 02-onboarding-completed-state
Status: Done

## Goal
The Compose Multiplatform app exists and launches on Android and iOS: it starts the shared core, uses the app's
dark theme, and shows a placeholder Home screen through the app's navigation graph. Nothing from onboarding is
visible yet; 04 adds the onboarding screen on top of this shell.

## Scope
### In scope
- Build support for the `apps/cmp` modules: Compose Multiplatform for UI modules, the app shell modules
  (shared app module, Android application, iOS host), and their place in the module rules
  (`docs/architecture.md` 5.2 / 9).
- `apps/cmp` shell: Android application and iOS host app that start the shared core (`initKoin`, with the Android
  context) and show the shared Compose app.
- Dark theme with the design's purple accents and Inter typography, available to every screen.
- Type-safe navigation graph with a placeholder Home destination as the start screen.
- App: `apps/cmp` (on `main`).
### Out of scope (handled later)
- The onboarding screen, start-screen choice and "Acessar" -> 04-cmp-onboarding-happy-path
- The real Home screen -> Home feature
- `apps/native` shells -> 06-native-android-app-shell, 07-native-ios-app-shell

## Acceptance criteria
None from task.md directly: this subtask is the prerequisite for AC-1–AC-8 on the cmp app. It provides the
placeholder Home that AC-2 opens until the Home feature exists.

## Edge cases
None from task.md; EC-1–EC-6 are handled in 04 / 05.

## Layers touched
- Build logic (app and Compose module setup, module rules)
- App shell (Android application, iOS host, shared app entry)
- Theme (dark, purple accent, typography)
- Navigation (graph, placeholder Home)

## How to test
1. Build and run the Android app on an emulator: it launches without crashing and shows the placeholder Home in
   the dark theme.
2. Build and run the iOS app on the simulator: same result.
3. Run the JVM checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`): pass, including the module
   rules for the new `apps/cmp` modules.

## Change log
- 2026-11-11: Initial version (prerequisite split out of the onboarding happy path).
