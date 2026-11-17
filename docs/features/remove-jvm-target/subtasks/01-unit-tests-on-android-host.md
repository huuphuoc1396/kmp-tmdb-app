# 01 - Unit tests on the Android host

Feature: [task.md](../task.md)
Depends on: none
Status: Done

## Goal
Every unit test of the KMP build runs as an Android host test, and CI and the coverage gate use those tests. The
JVM target still exists after this subtask, so the build keeps working; it is removed in 02.

## Scope
### In scope
- Android host tests enabled for shared and cmp modules; the unit test command runs them.
- Existing JVM unit tests moved to Android host tests:
  - Tests of code that uses the master key cipher (encrypted DataStore, settings repository) use a MockK cipher
    that passes data through.
  - The shared-core start-up (Koin wiring) test becomes an Android device test.
  - The iOS entry point test runs on the iOS simulator.
- The cmp Compose UI tests (onboarding content, navigation) removed; cmp ViewModel tests run on the Android host.
- The coverage gate (80% line on `domain` and `data`) measures Android host tests.
- CI runs Android host tests instead of JVM tests.
- `commonTest` tests run on the Android host and the iOS simulator.
### Out of scope (handled later)
- Removing the JVM target and the JVM-only production code -> 02-remove-jvm-target
- Docs and rules -> 03-docs-for-android-and-ios-testing

## Acceptance criteria
- **AC-2** Unit tests run on the Android host
  - Given the KMP build
  - When the developer runs the unit test command
  - Then every module's unit tests run as Android host tests and pass, without an emulator, simulator or Xcode
  - And the build-logic tests and the architecture tests still run and pass
- **AC-3** No fake security code in production (partial: test side only)
  - Host tests that need a cipher use a MockK cipher
  - And the real Android Keystore cipher is covered by Android device tests
  - And the iOS Keychain path is covered by the `native-ios` Swift tests in the app's test host
  - (Removing the in-memory production cipher is in 02.)
- **AC-4** cmp Compose UI tests removed
  - Given the cmp app
  - Then it has no Compose UI tests
  - And its ViewModel tests run as Android host tests
- **AC-5** Coverage gate kept
  - Given a `domain` or `data` module
  - When the coverage check runs
  - Then it measures Android host tests and fails below 80% line coverage
- **AC-6** CI on Ubuntu (partial: CI switched to Android host tests; confirmed without the JVM target in 02)
  - Given a pull request to `main`
  - Then CI runs static analysis, Android host tests, the coverage gate and the architecture tests on Ubuntu,
    and they pass

## Edge cases
- **EC-1** A test needs the real Android Keystore -> it is an Android device test, not a host test; host tests
  never touch the Keystore.
- **EC-3** A test written in `commonTest` -> it runs on both the Android host and the iOS simulator; it uses no
  MockK and no platform-only API.

## Layers touched
- Build logic (test and coverage setup)
- Unit tests of the shared core and the cmp app
- CI workflow

## How to test
1. Run the unit test command: all Android host tests pass, with no emulator or simulator running (AC-2).
2. Run the build-logic tests and the architecture tests: they pass (AC-2).
3. Run the coverage check: it passes, and lowering a `data` module's tests below 80% makes it fail (AC-5).
4. Check the cmp modules: no Compose UI tests remain; the ViewModel tests run on the host (AC-4).
5. On an emulator, run the Android device tests of the security module and of the start-up test: they pass
   (AC-3, EC-1).
6. On a simulator, run the iOS tests of the iOS entry point and the `commonTest` tests: they pass (EC-3).
7. Open a PR: CI runs the four checks on Ubuntu and passes (AC-6).

## Change log
- 2026-11-17: Initial version.
