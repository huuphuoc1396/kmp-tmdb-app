# Remove the JVM target

Status: Confirmed
Source: chat (`/spark` request, 2026-11-17)

## Goal
The product ships only on Android and iOS, but the shared core and the cmp app also build a JVM target. That
target exists only to run unit tests fast. It needs fake production code (an in-memory master key and a
JVM-only settings file path), it is one more target to compile and publish, and it runs tests away from the real
platforms.

After this change:
- The KMP build targets only Android and iOS.
- Unit tests run as Android host tests by default.
- The real platform security code is verified on its platform.

Success: no JVM target and no JVM-only production code remain, and CI still runs static analysis, unit tests, the
coverage gate and the architecture tests on every pull request.

## Scope
### In scope
- **Targets:** remove the JVM target from every shared module and from the cmp app. Delete the JVM-only
  production code. The published shared core has no JVM artifacts.
- **Default unit tests:** they run as Android host tests (on the developer's JVM, MockK available). One command
  runs all of them.
- **Encrypted storage:**
  - Production keeps only the real Android Keystore and iOS Keychain implementations.
  - Host tests of code that uses the master key cipher (encrypted DataStore, repositories) use a MockK cipher
    that passes data through unchanged.
  - The real Android Keystore is verified by Android device tests.
  - The real iOS Keychain path is verified by the Swift tests of `native-ios`, which run in the app's test host.
- **Tests that need the real platform:**
  - The shared-core start-up test (Koin wiring) runs as an Android device test.
  - The iOS entry point test runs on the iOS simulator. It only resolves objects and does not touch the
    Keychain.
- **cmp Compose UI tests:** the onboarding content and navigation UI tests are removed. The UI is checked
  manually, as in the native apps.
- **Coverage:** the 80% line coverage gate on `domain` and `data` modules stays, measured on Android host
  tests.
- **CI:** still Ubuntu only. It runs static analysis, Android host tests, the coverage gate and the architecture
  tests.
- **`commonTest`:** stays allowed for tests that use only multiplatform libraries (no MockK). These tests run on
  the Android host and on the iOS simulator.
- **Docs:** the architecture doc, the project guide and the rules describe the new targets and the testing
  strategy.

### Out of scope
- A desktop app.
- A macOS CI job and iOS tests on CI.
- A test host for Kotlin iOS tests, so that they could reach the Keychain.
- Robolectric.
- Build-logic unit tests and the architecture tests (Konsist): they are JVM projects, not KMP targets, and stay
  as they are.
- `native-android/` and `native-ios/`: they consume only the Android and iOS artifacts. They are unchanged,
  apart from republishing.

## User flow
For a developer:
1. Run the unit tests with one command: they run as Android host tests, without an emulator, simulator or Xcode.
2. Before opening a PR, run the CI checks locally: static analysis, host tests, the coverage gate and the
   architecture tests.
3. Change platform security code: run the Android device tests on an emulator, and the `native-ios` tests on
   a simulator, both locally.

## UI states
Not applicable (build and test infrastructure; no screen changes).

## Acceptance criteria
- **AC-1** Only Android and iOS targets
  - Given the KMP build
  - Then every shared module and every cmp module targets only Android and iOS
  - And there is no JVM-only production code
  - And publishing the shared core produces no JVM artifacts

- **AC-2** Unit tests run on the Android host
  - Given the KMP build
  - When the developer runs the unit test command
  - Then every module's unit tests run as Android host tests and pass, without an emulator, simulator or Xcode
  - And the build-logic tests and the architecture tests still run and pass

- **AC-3** No fake security code in production
  - Given the production sources
  - Then the master key cipher has only the Android Keystore and iOS Keychain implementations
  - And host tests that need a cipher use a MockK cipher
  - And the real Android Keystore cipher is covered by Android device tests
  - And the iOS Keychain path is covered by the `native-ios` Swift tests in the app's test host

- **AC-4** cmp Compose UI tests removed
  - Given the cmp app
  - Then it has no Compose UI tests
  - And its ViewModel tests run as Android host tests

- **AC-5** Coverage gate kept
  - Given a `domain` or `data` module
  - When the coverage check runs
  - Then it measures Android host tests and fails below 80% line coverage

- **AC-6** CI on Ubuntu
  - Given a pull request to `main`
  - Then CI runs static analysis, Android host tests, the coverage gate and the architecture tests on Ubuntu,
    and they pass

- **AC-7** Docs describe the new setup
  - Given the architecture doc, the project guide and the rules
  - Then they describe the Android and iOS targets, where each kind of test runs, and the commands to run them
  - And they no longer mention a JVM target or JVM unit tests for shared modules

## Edge cases
- **EC-1** A test needs the real Android Keystore -> it is an Android device test, not a host test; host tests
  never touch the Keystore.
- **EC-2** A Kotlin test on iOS would need the Keychain -> it is not written as a Kotlin iOS test (no app
  bundle there); the behavior is covered by the `native-ios` Swift tests.
- **EC-3** A test written in `commonTest` -> it runs on both the Android host and the iOS simulator; it uses no
  MockK and no platform-only API.

## Constraints
- Android and iOS levels are unchanged (minSdk 24, iOS 16).
- CI stays on Ubuntu, with no macOS runner.
- Platform tests (Android device, iOS simulator, `native-ios`) run locally.
- No new test library (no Robolectric, no Mokkery). MockK stays limited to host tests.
- Existing app behavior does not change.

## Open questions
- None.

## Change log
- 2026-11-17: Initial version.

## Subtasks
1. [01 - Unit tests on the Android host](subtasks/01-unit-tests-on-android-host.md)
2. [02 - Remove the JVM target](subtasks/02-remove-jvm-target.md)
3. [03 - Docs for Android and iOS testing](subtasks/03-docs-for-android-and-ios-testing.md)
