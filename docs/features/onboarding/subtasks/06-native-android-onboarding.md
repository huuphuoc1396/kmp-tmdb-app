# 06 - Onboarding on Android (native)

Feature: [task.md](../task.md)
Design: [Onboarding](../design/onboarding.md)
Depends on: 02-onboarding-completed-state. Prerequisite outside this feature: the standalone native Android app ([native-android-app](../../native-android-app/task.md): published shared core and the `native-android/` shell).
Status: Todo

## Goal
On the native Android app (Jetpack Compose), onboarding works end to end as designed: first launch shows it, "Acessar" opens Home, later launches open Home directly. It is accessible, adapts to orientation and screen size, works offline, and is in en / pt-BR.

## Scope
### In scope
- Onboarding screen as in the design, edge-to-edge, with the gradient placeholder artwork and TMDB's official logo.
- en and pt-BR copy (copy in design/onboarding.md).
- Start screen chosen from the onboarding completed state, with no flash of the wrong screen.
- "Acessar" records completion and replaces onboarding with Home; a placeholder Home until the Home feature exists.
- Accessibility, large fonts, landscape / tablet, rotation and process death.
- App: the standalone `native-android/` project, using the shared core through the published libraries (`./gradlew publishSharedCore` first).
### Out of scope (handled later)
- iOS -> 07-native-ios-onboarding
- The real Home screen -> Home feature

## Acceptance criteria
- **AC-1** Onboarding shown on first launch
  - Given the app is freshly installed
  - When the user launches it
  - Then the onboarding screen shows the full-screen artwork, headline, value proposition and primary button
- **AC-2** Button enters the app
  - Given the onboarding screen is shown
  - When the user taps the primary button
  - Then Home is shown (until the Home feature exists: a placeholder Home destination)
- **AC-3** Onboarding skipped after completion
  - Given the user has completed onboarding before
  - When the user launches the app again (including after closing it fully)
  - Then the app opens on Home, with no onboarding screen shown even briefly
- **AC-4** Onboarding is not in the back stack
  - Given the user tapped the primary button and is on Home
  - When the user presses system Back or swipes back
  - Then onboarding is not shown again
  - And given the user is on onboarding, when they press Back, then the app is left (as on any root screen)
- **AC-5** Localized copy
  - Given the device language is pt-BR
  - When onboarding is shown
  - Then the headline and value proposition are in Portuguese and the button reads "Acessar"
  - And given any other device language, then all copy is in English
- **AC-6** Works offline
  - Given the device has no network connection
  - When onboarding is shown and the user taps the primary button
  - Then the screen shows fully (artwork included) and Home opens, with no network request made by onboarding
- **AC-7** Accessible
  - Given a screen reader is on
  - When onboarding is shown
  - Then the artwork is skipped as decorative, the headline and value proposition are read, and the button is
    announced as a button with its label
  - And given the largest system font size, then all text stays readable without clipping
  - And the button meets the platform's minimum touch target size and text contrast guidelines
- **AC-8** Orientation and screen sizes
  - Given a phone or tablet in portrait or landscape
  - When onboarding is shown
  - Then the artwork fills the screen (cropped, not stretched or letterboxed) and the text and button are fully
    visible or reachable

## Edge cases
- **EC-1** Saving "onboarding completed" fails -> the user still enters Home; onboarding may show again on the
  next launch. No error is shown.
- **EC-2** App closed, backgrounded or killed before the button is tapped -> onboarding shows again on the next
  launch.
- **EC-3** Button tapped repeatedly or double-tapped quickly -> Home opens exactly once.
- **EC-4** Rotation or process death while on onboarding -> onboarding is restored as it was; no state is lost.
- **EC-5** Long translated copy or a very large font size -> text wraps (and scrolls if needed); the button stays
  reachable.
- **EC-6** Device language is neither en nor pt-BR -> English copy is shown.

## Layers touched
- UI (Jetpack Compose screen, design-system pieces it needs)
- State holder (ViewModel)
- Navigation / start screen
- Android resources (strings en + pt-BR, artwork, logo)

## How to test
1. Fresh install, launch: onboarding shows artwork, logo, headline, body and the button, matching the design (AC-1).
2. Tap the button: the placeholder Home opens. On a fresh install, double-tap quickly: Home opens once (AC-2, EC-3).
3. On Home, press Back: the app is left and onboarding does not reappear. On onboarding, Back leaves the app (AC-4).
4. Kill and relaunch: the app opens on Home with no flash of onboarding (AC-3).
5. Fresh install, kill the app before tapping, relaunch: onboarding shows again (EC-2).
6. Device language pt-BR: Portuguese copy and "Acessar". English and an unsupported language (e.g. French):
   English copy (AC-5, EC-6).
7. Airplane mode, fresh install: onboarding shows fully and the button opens Home (AC-6).
8. Force the save to fail (debug build or test): Home still opens with no error shown; relaunch shows onboarding (EC-1).
9. Screen reader on: artwork and logo are skipped, headline and body are read, the button is announced as a
   button with its label (AC-7).
10. Largest system font size: text wraps, the content scrolls if needed, the button stays reachable (AC-7, EC-5).
11. Phone and tablet, portrait and landscape: artwork fills the screen with a cover crop; text and button are
   visible or reachable (AC-8).
12. Rotate on onboarding, and trigger process death ("Don't keep activities"): onboarding is restored as it was (EC-4).

Run on an Android emulator or device (TalkBack for accessibility).

## Change log
- 2026-11-10: Initial version.
- 2026-11-11: Renumbered from 05; apps are directories on `main`; copy and assets resolved.
- 2026-11-12: Target moved from `apps/native` to the standalone `native-android/` app (feature native-android-app).
