# 02 - Onboarding completed state

Feature: [task.md](../task.md)
Depends on: 01-encrypted-settings-storage
Status: Done

## Goal
The shared core can record that the user completed onboarding and tell the apps, at start-up and as it
changes, whether it is completed. This is the public API both UI branches use to decide the start screen.

## Scope
### In scope
- Record "onboarding completed" on the device.
- Report whether onboarding is completed, as a value that updates when it changes; "not completed" when
  nothing was saved yet.
- A failed save is returned as an error result, not thrown.
- Wiring so the apps can obtain these operations through dependency injection.
- Branch: `main`.
### Out of scope (handled later)
- Choosing the start screen and the onboarding UI -> 03-cmp-onboarding-happy-path, 05-native-android-onboarding,
  06-native-ios-onboarding

## Acceptance criteria
- **AC-3** Onboarding skipped after completion (partial: the core reports "completed" after it was recorded,
  including after a restart; the start screen choice is in 03 / 05 / 06)
  - Given the user has completed onboarding before
  - When the user launches the app again (including after closing it fully)
  - Then the app opens on Home, with no onboarding screen shown even briefly
- **AC-6** Works offline (partial: recording and reading the state makes no network request; the screen is
  in 03 / 05 / 06)
  - Given the device has no network connection
  - When onboarding is shown and the user taps the primary button
  - Then the screen shows fully (artwork included) and Home opens, with no network request made by onboarding

## Edge cases
- **EC-1** Saving "onboarding completed" fails -> the user still enters Home; onboarding may show again on the
  next launch. No error is shown. (partial: the failure is returned as an error result and the state stays
  "not completed"; the UI behavior is in 03 / 05 / 06)
- **EC-2** App closed, backgrounded or killed before the button is tapped -> onboarding shows again on the next
  launch. (partial: with nothing recorded, the core reports "not completed")

## Layers touched
- Use cases (domain)
- Repository (data)
- Dependency injection

## How to test
Nothing to run in an app yet (`main` has no UI). Check through tests:
1. Fresh storage: the state reads "not completed".
2. Record completion: the state changes to "completed" for an existing observer.
3. Recreate the core on the same storage: the state reads "completed".
4. Make the save fail: an error result is returned and the state stays "not completed".

## Change log
- 2026-11-10: Initial version.
