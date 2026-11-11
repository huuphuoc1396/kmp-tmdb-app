# 01 - Encrypted settings storage

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
The shared core can save small app settings on the device and read them back, encrypted at rest, on Android,
iOS and JVM. Nothing is visible to the user yet; this is the storage that the onboarding state (02) builds on.

## Scope
### In scope
- A device master key on each platform (Android, iOS, and a JVM stand-in for tests).
- Encrypting and decrypting stored settings with that key.
- A settings store in the shared core that reads and writes encrypted settings and exposes changes as they
  happen.
- Read and write failures are reported as errors, never as a crash or silently lost data.
- Branch: `main`.
### Out of scope (handled later)
- The onboarding completed setting and its use cases -> 02-onboarding-completed-state
- Any UI -> 03, 05, 06

## Acceptance criteria
- **AC-3** Onboarding skipped after completion (partial: provides the persistent, on-device storage that
  keeps a saved setting across app restarts; the onboarding flag itself is in 02, the start screen in 03 / 05 / 06)
  - Given the user has completed onboarding before
  - When the user launches the app again (including after closing it fully)
  - Then the app opens on Home, with no onboarding screen shown even briefly

## Edge cases
- **EC-1** Saving "onboarding completed" fails -> the user still enters Home; onboarding may show again on the
  next launch. No error is shown. (partial: a failed write is reported as an error to the caller instead of
  crashing; the handling is in 02 and the UI subtasks)

## Layers touched
- Platform key storage (Android, iOS, JVM)
- Encryption
- Local settings store

## How to test
Nothing to run in an app yet (`main` has no UI). Check through tests:
1. Write a setting, read it back: the value matches.
2. Recreate the store on the same storage (simulating an app restart): the value is still there.
3. Inspect the stored file: the value does not appear in plain text.
4. Make the write fail: the caller receives an error and the app does not crash.
5. On iOS, run the platform tests on a simulator for the key storage; on Android, the key storage is checked
   with platform tests when they are configured.

## Notes
- Size risk: larger than the ~400-line / 3-module guideline (platform key storage on three targets plus the
  encrypted store). Kept as one subtask by choice (2026-11-10).

## Change log
- 2026-11-10: Initial version.
