# 03 - Onboarding happy path (cmp)

Feature: [task.md](../task.md)
Design: [Onboarding](../design/onboarding.md)
Depends on: 02-onboarding-completed-state. Prerequisite outside this feature: the cmp app shell (app module, dark theme, navigation) on the `cmp` branch.
Status: Todo

## Goal
On the Compose Multiplatform app (Android and iOS), a first launch shows the onboarding screen as designed, "Acessar" opens Home, and later launches open Home directly. It works offline and in en / pt-BR.

## Scope
### In scope
- Onboarding screen as in the design: artwork, scrim, logo, headline, body, primary button, edge-to-edge.
- Bundled artwork and logo; en and pt-BR copy (English copy from task.md Open questions).
- Start screen chosen from the onboarding completed state, with no flash of the wrong screen.
- "Acessar" records completion and replaces onboarding with Home; a placeholder Home until the Home feature exists.
- Branch: `cmp`.
### Out of scope (handled later)
- Accessibility, landscape / tablet layout, large fonts, rotation and process death -> 04-cmp-onboarding-robustness
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

## Edge cases
- **EC-1** Saving "onboarding completed" fails -> the user still enters Home; onboarding may show again on the
  next launch. No error is shown.
- **EC-2** App closed, backgrounded or killed before the button is tapped -> onboarding shows again on the next
  launch.
- **EC-3** Button tapped repeatedly or double-tapped quickly -> Home opens exactly once.
- **EC-6** Device language is neither en nor pt-BR -> English copy is shown.

## Layers touched
- UI (screen, design-system pieces it needs)
- State holder (ViewModel)
- Navigation / start screen
- Resources (strings en + pt-BR, artwork, logo)

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

Run on Android and on the iOS simulator.

## Change log
- 2026-11-10: Initial version.
