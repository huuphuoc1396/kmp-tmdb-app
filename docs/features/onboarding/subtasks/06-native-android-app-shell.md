# 06 - App shell (native Android)

Feature: [task.md](../task.md)
Depends on: 02-onboarding-completed-state
Status: Done

## Goal
The native Android app (Jetpack Compose) exists in `apps/native` and launches. It starts the shared core, uses the
app's dark theme, and shows a placeholder Home screen through its navigation graph. Nothing from onboarding is
visible yet; 08 adds the onboarding screen on top of this shell.

## Scope
### In scope
- Build support for the `apps/native` Android modules (`app`, `designsystem`, and later `feature/<x>/{nav,api,ui}`),
  and their place in the module rules (`docs/architecture.md` 5.2 / 9, 11).
  - Jetpack Compose, not Compose Multiplatform.
  - `apps/native` and `apps/cmp` never depend on each other.
- `apps/native/app`, the Android application:
  - `Application` that starts the shared core (`initKoin` with the Android context).
  - `Activity` that is edge-to-edge with light system bar icons and shows the app's navigation graph.
- `apps/native/designsystem`: dark theme with the design's purple accents. It uses the same values as
  `apps/cmp/designsystem`, but the code is written separately, because the two apps share nothing.
- Type-safe navigation graph (Navigation Compose) with a placeholder Home destination as the start screen.
- App: `apps/native` (on `main`).
### Out of scope (handled later)
- The iOS app, the `shared` umbrella framework and SKIE → 07-native-ios-app-shell
- The onboarding screen, start-screen choice and "Acessar" → 08-native-android-onboarding
- The real Home screen → Home feature

## Acceptance criteria
None from task.md directly. This subtask is the prerequisite for AC-1–AC-8 on the native Android app, and it
provides the placeholder Home that AC-2 opens until the Home feature exists.

## Edge cases
None from task.md; EC-1–EC-6 are handled in 08.

## Layers touched
- Build logic (Android modules of `apps/native`, module rules)
- App shell (Android application, Activity, Koin start)
- Theme (dark, purple accent)
- Navigation (graph, placeholder Home)

## How to test
1. Build and run the native Android app on an emulator: it launches without crashing and shows the placeholder
   Home in the dark theme with light system bar icons.
2. Install it next to the cmp app: both install and run side by side. They are different applications with
   different application IDs.
3. Run the JVM checks (`./gradlew detekt jvmTest koverVerify :architecture-test:test`). They pass, including the
   module rules for the new `apps/native` modules (no dependency on `apps/cmp`).

## Change log
- 2026-11-12: Initial version (prerequisite split out of the native Android onboarding).
